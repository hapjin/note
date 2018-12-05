package net.hapjin.trie;

import java.util.HashMap;
import java.util.Map;

public class MapTrieST<Value> {
    private Node root;

    private static class Node{
        private Object val;
        private Map<Character, Node> next = new HashMap<>();
    }


    public Value get(String key) {
        Node x = get(root, key, 0);
//        if (x == null) {
//            return null;
//        }
        return (Value) x.val;
    }

    private Node get(Node x, String key, int d) {
        if (x == null) {
            return null;
        }
        if (d == key.length()) {
            return x;
        }
        Character ch = key.charAt(d);
        return get(x.next.get(ch), key, d + 1);
    }

    public void add(String key, Value val) {
        root = add(root, key, val, 0);
    }

    private Node add(Node x, String key, Value val, int d) {
        if (x == null) {
            x = new Node();
        }
        if (d == key.length()) {
            x.val=val;
            return x;
        }
        Character ch = key.charAt(d);
        x.next.put(ch, add(x.next.get(ch), key, val, d + 1));
        return x;
    }

    public static void main(String[] args) {
        MapTrieST<String> mapTrieST = new MapTrieST<>();
        mapTrieST.add("i", "1");
        mapTrieST.add("love", "2");
        mapTrieST.add("u", "3");

        System.out.println(mapTrieST.get("i"));
        System.out.println(mapTrieST.get("l"));
        System.out.println(mapTrieST.get("u"));

        System.out.println(mapTrieST.get("x"));

    }


}
