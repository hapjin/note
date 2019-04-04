### 自定义 ThreadPoolExecutor 处理线程运行时异常

最近看完了[ElasticSearch线程池模块](https://www.cnblogs.com/hapjin/p/10188056.html)的源码，感触颇深，然后也自不量力地借鉴ES的 EsThreadPoolExecutor 重新造了一把轮子，对线程池的理解又加深了一些。在继承 ThreadPoolExecutor实现自定义的线程池时，ES先重写了Runnable接口，提供了更灵活的任务运行过程中出现异常处理逻辑。简而言之，它采用回调机制实现了线程在运行过程中抛出未受检异常的统一处理逻辑，非常优美。实在忍不住把源码copy下来：

```java
/**
 * An extension to runnable.
 */
public abstract class AbstractRunnable implements Runnable {

    /**
     * Should the runnable force its execution in case it gets rejected?
     */
    public boolean isForceExecution() {
        return false;
    }

    @Override
    public final void run() {
        try {
            doRun();
        } catch (Exception t) {
            onFailure(t);
        } finally {
            onAfter();
        }
    }

    /**
     * This method is called in a finally block after successful execution
     * or on a rejection.
     */
    public void onAfter() {
        // nothing by default
    }

    /**
     * This method is invoked for all exception thrown by {@link #doRun()}
     */
    public abstract void onFailure(Exception e);

    /**
     * This should be executed if the thread-pool executing this action rejected the execution.
     * The default implementation forwards to {@link #onFailure(Exception)}
     */
    public void onRejection(Exception e) {
        onFailure(e);
    }

    /**
     * This method has the same semantics as {@link Runnable#run()}
     * @throws InterruptedException if the run method throws an InterruptedException
     */
    protected abstract void doRun() throws Exception;
}
```

1. 统一的任务执行入口方法doRun()，由各个子类实现doRun()执行具体的业务逻辑

2. try-catch中统一处理线程执行任务过程中抛出的异常，由onFailure()处理

3. 任务执行完成（不管是正常结束还是运行过程中抛出了异常），统一由onAfter()处理

4. 另外，还有一个`isForceExecution`方法，用来支持任务在提交给线程池被拒绝了，强制执行。当然了，这需要线程池的任务队列提供相关的支持。我也是受这种方式的启发，实现了一个**线程在执行任务过程中抛出未受检异常时，重新提交任务运行的线程池**。

   ​

此外，ES内置了好几个默认实现的线程池，比如 EsThreadPoolExecutor 、QueueResizingEsThreadPoolExecutor 和 PrioritizedEsThreadPoolExecutor。

1. QueueResizingEsThreadPoolExecutor 

   我们都知道创建线程池时会指定一个任务队列(BlockingQueue)，平常都是直接用 LinkedBlockingQueue，它是一个无界队列，当然也可以在构造方法中指定队列的长度。但是，**ES中几乎不用 LinkedBlockingQueue 作为任务队列，而是使用 LinkedTransferQueue **，但是 LinkedTransferQueue 又是一个无界队列，于是ES又基于LinkedTransferQueue 封装了一个任务队列，类名称为 ResizableBlockingQueue，它能够限制任务队列的长度。

   那么问题来了，对于一个线程池，任务队列设置为多长合适呢？

   答案就是Little's Law。在QueueResizingEsThreadPoolExecutor 线程池中重写了afterExecute()方法，里面统计了每个任务的运行时间、等待时间(入队列到执行)。所以，**你想知道如何统计一个任务的运行时间吗？**你想统计线程池一共提交了多少个任务，所有任务的运行时间吗？看看QueueResizingEsThreadPoolExecutor 源码就明白了。

   另外再提一个问题，为什么ES用 LinkedTransferQueue 作为任务队列而不用 LinkedBlockingQueue 呢？

   我想：很重要的一个原因是LinkedBlockingQueue 是基于重量级的锁实现的入队操作，而LinkedTransferQueue 是基于CAS原子指令实现的入队操作。那么这就是synchronized内置锁和CAS原子指令之间的一些差异了，你懂得。

2. PrioritizedEsThreadPoolExecutor

   优先级任务的线程池，任务提交给线程池后是在任务队列里面排队，FIFO模式。而这个线程池则允许任务定义一个优先级，优先级高的任务先执行。

3. EsThreadPoolExecutor 

   这个线程池最接近经常见到的ThreadPoolExecutor，不过，它实现了一些拒绝处理逻辑，提交任务若被拒绝(会抛出EsRejectedExecutionException异常)，则进行相关处理

   ```java
       @Override
       public void execute(final Runnable command) {
           doExecute(wrapRunnable(command));
       }

       protected void doExecute(final Runnable command) {
           try {
               super.execute(command);
           } catch (EsRejectedExecutionException ex) {
               if (command instanceof AbstractRunnable) {
                   // If we are an abstract runnable we can handle the rejection
                   // directly and don't need to rethrow it.
                   try {
                       ((AbstractRunnable) command).onRejection(ex);
                   } finally {
                       ((AbstractRunnable) command).onAfter();

                   }
               } else {
                   throw ex;
               }
           }
       }
   ```

   ​

讲完了ES中常用的三个线程池实现，还想结合JDK源码，**记录一下线程在执行任务过程中抛出运行时异常，是如何处理的。我觉得有二种方式（或者说有2个地方）来处理运行时异常**。一种方式是：java.util.concurrent.ThreadPoolExecutor#afterExecute方法，另一种方式是：java.lang.Thread.UncaughtExceptionHandler#uncaughtException

1. afterExecute

   看ThreadPoolExecutor#afterExecute(Runnable r, Throwable t) 的源码注释：

   >Method invoked upon completion of execution of the given Runnable.This method is invoked by the thread that executed the task. If non-null, the Throwable is the uncaught  RuntimeException or Error that caused execution to terminate abruptly.

   提交给线程池的任务，执行完（不管是正常结束，还是执行过程中出现了异常）后都会自动调用afterExecute()方法。如果执行过程中出现了异常，那么Throwable t 就不为null，并且导致执行终止(terminate abruptly.)。

   >This implementation does nothing, but may be customized in subclasses. Note: To properly nest multiple overridings, subclasses  should generally invoke  super.afterExecute  at the beginning of this method.

   默认的afterExecute(Runnable r, Throwable t) 方法是一个空实现，什么也没有。因此，在继承ThreadPoolExecutor实现自己的线程池时，如果重写该方法，则要记住：先调用 `super.afterExecute `

   比如说这样干：

   ```java
       @Override
       protected void afterExecute(Runnable r, Throwable t) {
           super.afterExecute(r, t);
           if (t != null) {
               //出现了异常
               if (r instanceof AbstractRunnable && ((AbstractRunnable)r).isForceExecution()) {
                   //TextAbstractRunnable 设置为强制执行时重新拉起任务
                   execute(r);
                   logger.error("AbstractRunnable task run time error:{}, restarted", t.getMessage());
               }
           }
       }
   ```

   看，重写afterExecute方法，当 Throwable 不为null时，表明线程执行任务过程中出现了异常，这时就重新提交任务。

   有个时候，在实现 Kafka 消费者线程的时候(while true循环)，经常因为解析消息出错导致线程抛出异常，就会导致 Kafka消费者线程挂掉，这样就永久丢失了一个消费者了。而通过这种方式，当消费者线程挂了时，可重新拉起一个新任务。

2. uncaughtException

   创建 ThreadPoolExecutor时，要传入ThreadFactory 作为参数，在而创建ThreadFactory 对象时，就可以设置线程的异常处理器java.lang.Thread.UncaughtExceptionHandler。

   [在用Google Guava包](https://www.cnblogs.com/hapjin/p/10012435.html)的时候，一般这么干：

   ```java
   //先 new  Thread.UncaughtExceptionHandler对象 exceptionHandler
   private ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("thread_name-%d").setUncaughtExceptionHandler(exceptionHandler).build();

   ```

   在线程执行任务过程中，如何抛出了异常，就会由JVM调用 Thread.UncaughtExceptionHandler 中实现的异常处理逻辑。看Thread.UncaughtExceptionHandler的JDK源码注释：

   >Interface for handlers invoked when a Thread abruptly. terminates due to an uncaught exception.
   >
   >When a thread is about to terminate due to an uncaught exception the Java Virtual Machine will query the thread for its UncaughtExceptionHandler using getUncaughtExceptionHandler and will invoke the handler's uncaughtException method, passing the thread and the exception as arguments.

   其大意就是：如果线程在执行Runnable任务过程因为 uncaught exception 而终止了，那么 JVM 就会调用getUncaughtExceptionHandler 方法查找是否设置了异常处理器，如果设置了，那就就会调用异常处理器的java.lang.Thread.UncaughtExceptionHandler#uncaughtException方法，这样我们就可以在这个方法里面定义异常处理逻辑了。

   ​

### 总结

ES的ThreadPool 模块是学习线程池的非常好的一个示例，实践出真知。它告诉你如何自定义线程池（用什么任务队列？cpu核数、任务队列长度等参数如何配置？）。在实现自定义任务队列过程中，也进一步理解了CAS操作的原理，如何巧妙地使用CAS？是失败重试呢？还是直接返回？

另外，线程在执行Runnable任务过程中抛出了异常如何处理？这里提到了Thread.UncaughtExceptionHandler#uncaughtException 和 ThreadPoolExecutor#afterExecute。前者是由JVM自动调用的，后者则是在每个任务执行结束后都会被调用。

另外，注意：Thread.UncaughtExceptionHandler#uncaughtException  和 RejectedExecutionHandler#rejectedExecution 是不同的。RejectedExecutionHandler 用来处理任务在提交的时候，被线程池拒绝了，该怎么办的问题，默认是AbortPolicy，即：直接丢弃。



