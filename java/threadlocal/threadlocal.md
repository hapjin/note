### 一种解决 ThreadLocal 内存不能及时回收的方法

在看了[深入理解ThreadLocal](https://www.cnblogs.com/noteless/p/10373044.html#10)这篇文章之后，对ThreadLocal原理的理解更深了一些，发现网上有很多讨论ThreadLocal“内存泄漏”的问题，其解决方案是：及时调用remove()方法。此外，ThreadLocal类本身也在执行`java.lang.ThreadLocal#get`和`java.lang.ThreadLocal#set`的时候内部会调用`java.lang.ThreadLocal.ThreadLocalMap#expungeStaleEntry`及时清理不再使用的value。而最近刚好在看ES源码，发现 Lucene 工具包里面org.apache.lucene.util.CloseableThreadLocal 针对“内存泄漏”问题实现了一种新的解决方案。

也许有人认为ThreadLocal并不存在“内存泄漏”问题，仁者见仁，争论这个问题意义不大，关键是理解背后的原理。引用一张图：

![threadlocal](F:\note\github\note\java\threadlocal\threadlocal.png)

每个线程Thread 内部有个 ThreadLocalMap 对象，定义如下：

```java
    /* ThreadLocal values pertaining to this thread. This map is maintained
     * by the ThreadLocal class. */
    ThreadLocal.ThreadLocalMap threadLocals = null;
```

ThreadLocalMap里面有个Entry数组，当执行`threadLocal.set(bigHeapObject);`那么：key指向threadLocal对象，key是一个弱引用，如上图虚线所示。而value则指向 我们向ThreadLocal中设置(set)的值，也即：bigHeapObject。

看下面的JDK源码，就知道为什么 key 是一个弱引用了：`super(k);`把 key 用 WeakReference“包装“起来。

```java
        static class Entry extends WeakReference<ThreadLocal<?>> {
            /** The value associated with this ThreadLocal. */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }
```



那为什么会 value 所指向的对象(bigHeapObject)不能及时回收呢？

随着线程的运行，当栈中的ThreadLocalRef引用断开连接了，由于Entry中的key是个WeakReference，那么堆上的ThreadLocal对象能够及时被GC掉，其实我认为：重点并不是ThreadLocal对象能否被及时GC掉（虽然[这篇文章](https://blog.csdn.net/xlgen157387/article/details/78298840)很好地分析了为什么使用弱引用），而是：

>如果线程一直存活着，栈上的 currentThreadRef ---> ThreadLoal.ThreadLocalMap--->value--->bigHeapObject内存空间 这条引用一直还在，根据"可达性"分析，导致JVM无法回收bigHeapObject对象，而这个对象从程序运行的角度来说，它已经不再使用了（或者当threadLocal对象都没了，就不会再去使用 bigHeapObject对象了），但是bigHeapObject却一直无法回收。

既然知道了 内存没有及时回收 的原因，而本文开头也提到了"ThreadLocal式"的解决方案：

- ThreadLocal类本身也在执行`java.lang.ThreadLocal#get`和`java.lang.ThreadLocal#set`的时候内部会调用`java.lang.ThreadLocal.ThreadLocalMap#expungeStaleEntry`及时清理不再使用的value（换句话说就是：不用咱们管，反正你调用get/set的时候，我尽量帮你去清理一下）

  该方案的缺点是：并不能保证不再使用的Entry在调用get或者set方法的时候一定会清理，它是以一定概率能够清除到不再使用的Entry的。看JDK源码：

  执行java.lang.ThreadLocal#get方法

  ```java
      public T get() {
          Thread t = Thread.currentThread();
          ThreadLocalMap map = getMap(t);
          if (map != null) {
              ThreadLocalMap.Entry e = map.getEntry(this);
              if (e != null) {
                  @SuppressWarnings("unchecked")
                  T result = (T)e.value;
                  return result;
              }
          }
          return setInitialValue();
      }
  ```

  在`ThreadLocalMap.Entry e = map.getEntry(this);`假设发生了哈希碰撞：`e!=null`但是`e.get()==key`不成立，于是执行`getEntryAfterMiss()`

  ```java
  private Entry getEntry(ThreadLocal<?> key) {
              int i = key.threadLocalHashCode & (table.length - 1);
              Entry e = table[i];
              if (e != null && e.get() == key)
                  return e;
              else
                  return getEntryAfterMiss(key, i, e);
          }
  ```

  ​

  `ThreadLocal<?> k = e.get()`当 `k==null`时，才会调用`expungeStaleEntry()`进行清理。否则的话执行`nextIndex(i, len)`线性探测法，继续探测下一个地址。（[这篇文章](https://www.cnblogs.com/micrari/p/6790229.html)提到了ThreadLocalMap的Map结构是个"环形Entry数组"，采用线性探测法解决hash冲突问题）

  ```java
  private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
              Entry[] tab = table;
              int len = tab.length;

              while (e != null) {
                  ThreadLocal<?> k = e.get();
                  if (k == key)
                      return e;
                  if (k == null)
                      expungeStaleEntry(i);
                  else
                      i = nextIndex(i, len);
                  e = tab[i];
              }
              return null;
          }
  ```

  因此，执行`expungeStaleEntry(i)`进行清除，是一个概率问题。如果，探测到下一个Entry的key不为null，那就不会去清理了。

  也正是这个原因，在Lucene CloseableThreadLocal 源码注释中提到：

  ```
  Java's builtin ThreadLocal has a serious flaw:it can take an arbitrarily long amount of time to  dereference the things you had stored in it, even once the ThreadLocal instance itself is no longer referenced.This is because there is single, master map stored for each thread, which all ThreadLocals share, and that master map only periodically purges "stale" entries.
  ```

  只是周期性地 清理 过时的 entries

  ​

  ```
  While not technically a memory leak, because eventually the memory will be reclaimed, it can take a long time and you can easily hit OutOfMemoryError because from the GC's standpoint the stale entries are not reclaimable.
  ```

  Lucene开发人员也认为这不是一个 memory leak 问题 :) ，毕竟这些stale对象最终会被回收。但是，正是因为这种"不确定性"，导致程序出现OOM

  ​

- 显示地在程序中调用remove()

  这种方式有一点限制了程序开发的灵活性。

### Lucene 中的解决方案

由于代码也不是很长，就直接贴全部代码了：

```java
public class CloseableThreadLocal<T> implements Closeable {

  private ThreadLocal<WeakReference<T>> t = new ThreadLocal<>();

  // Use a WeakHashMap so that if a Thread exits and is
  // GC'able, its entry may be removed:
  private Map<Thread,T> hardRefs = new WeakHashMap<>();
  
  // Increase this to decrease frequency of purging in get:
  private static int PURGE_MULTIPLIER = 20;

  // On each get or set we decrement this; when it hits 0 we
  // purge.  After purge, we set this to
  // PURGE_MULTIPLIER * stillAliveCount.  This keeps
  // amortized cost of purging linear.
  private final AtomicInteger countUntilPurge = new AtomicInteger(PURGE_MULTIPLIER);

  protected T initialValue() {
    return null;
  }
  
  public T get() {
    WeakReference<T> weakRef = t.get();
    if (weakRef == null) {
      T iv = initialValue();
      if (iv != null) {
        set(iv);
        return iv;
      } else {
        return null;
      }
    } else {
      maybePurge();
      return weakRef.get();
    }
  }

  public void set(T object) {

    t.set(new WeakReference<>(object));

    synchronized(hardRefs) {
      hardRefs.put(Thread.currentThread(), object);
      maybePurge();
    }
  }

  private void maybePurge() {
    if (countUntilPurge.getAndDecrement() == 0) {
      purge();
    }
  }

  // Purge dead threads
  private void purge() {
    synchronized(hardRefs) {
      int stillAliveCount = 0;
      for (Iterator<Thread> it = hardRefs.keySet().iterator(); it.hasNext();) {
        final Thread t = it.next();
        if (!t.isAlive()) {
          it.remove();
        } else {
          stillAliveCount++;
        }
      }
      int nextCount = (1+stillAliveCount) * PURGE_MULTIPLIER;
      if (nextCount <= 0) {
        // defensive: int overflow!
        nextCount = 1000000;
      }
      
      countUntilPurge.set(nextCount);
    }
  }

  @Override
  public void close() {
    // Clear the hard refs; then, the only remaining refs to
    // all values we were storing are weak (unless somewhere
    // else is still using them) and so GC may reclaim them:
    hardRefs = null;
    // Take care of the current thread right now; others will be
    // taken care of via the WeakReferences.
    if (t != null) {
      t.remove();
    }
    t = null;
  }
}
```

看其注释：

```
This class works around that, by only enrolling WeakReference values into the ThreadLocal, and separately holding a hard reference to each stored value.  When you call close , these hard references are cleared and then GC is freely able to reclaim space by objects stored in it.
We can not rely on ThreadLocal#remove() as it only removes the value for the caller thread, whereas org.apache.lucene.util.CloseableThreadLocal#close takes care of all threads.
You should not call CloseableThreadLocal#close until all threads are done using the instance.
```



我的理解：Lucene嫌JDK里面的ThreadLocal 清理过时对象 不及时、不可控、不喜欢这种不确定的感觉，哈哈。。。

于是，定义了一个变量countUntilPurge，只要它的值减少到0，就来一次清理。

```java
private final AtomicInteger countUntilPurge = new AtomicInteger(PURGE_MULTIPLIER);
```

看，只要线程不存活了(isAlive()方法)，就从WeakHashMap中删除该线程的引用。

```java
  // Purge dead threads
  private void purge() {
    synchronized(hardRefs) {
      int stillAliveCount = 0;
      for (Iterator<Thread> it = hardRefs.keySet().iterator(); it.hasNext();) {
        final Thread t = it.next();
        if (!t.isAlive()) {
          it.remove();
        } else {
          stillAliveCount++;
        }
      }
      int nextCount = (1+stillAliveCount) * PURGE_MULTIPLIER;
      if (nextCount <= 0) {
        // defensive: int overflow!
        nextCount = 1000000;
      }
      
      countUntilPurge.set(nextCount);
    }
  }
```



根据WeakHashMap的特征：

>  An entry in a WeakHashMap will automatically be removed when  its key is no longer in ordinary use.  More precisely, the presence of a  mapping for a given key will not prevent the key from being discarded by the
>  garbage collector, that is, made finalizable, finalized, and then reclaimed.
>  When a key has been discarded its entry is effectively removed from the map,  so this class behaves somewhat differently from other Map  implementations.

并且由于 value 也使用WeakReference也包装`private ThreadLocal<WeakReference<T>> t = new ThreadLocal<>();`，那么在下一次GC时，就能清理掉 stale entries了。但是由于，value 也使用WeakReference来包装了，那么：就需要定义 hard reference 来显示地持有线程引用：???（深入理解一下WeakHashMap）

```java
  // Use a WeakHashMap so that if a Thread exits and is
  // GC'able, its entry may be removed:
  private Map<Thread,T> hardRefs = new WeakHashMap<>();
```



另外，当所有的线程都执行完毕时，才能调用close()。从这里可看出在使用CloseableThreadLocal时，我们应当有意识地去管理线程，判断什么时候线程执行完毕了（一般情况就是Runnable#run方法执行结束）。