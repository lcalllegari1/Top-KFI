import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Map.Entry;

public class TopKFI {
    private static boolean DEBUG = true;

    private static int K, M;
    private static int TRANSACTION_INDEX = 0, MAX_VALUE = 0;

    private static String path;

    private static final HashMap<Integer, BitSet> map = new HashMap<>(1000);
    private static PriorityQueue<Itemset> Q;
    private static ArrayList<Itemset> S;

    public static void main(String[] args) {
        parseInputArgs(args);

        long t = System.currentTimeMillis();
        long startingTime = t;

        if (!parseDataset()) {
            return;
        }

        if (DEBUG)
            System.out.println("dataset reading time: " + (System.currentTimeMillis() - t));

        Q = new PriorityQueue<>(K, Collections.reverseOrder());
        S = new ArrayList<>(K);

        t = System.currentTimeMillis();
        int referenceSupport = initializeQ();
        clearMap(referenceSupport);
        if (DEBUG)
            System.out.println("queue init + map clear time: " + (System.currentTimeMillis() - t));

        // 4)

        for (int i = 0; i < K && !Q.isEmpty(); i++) {

            Itemset currentItemset = Q.poll();
            BitSet currentItemsetIndexes = currentItemset.getIndexes();
            
            S.add(currentItemset);

            for (int j = currentItemset.getMaxValueItem() + 1; j <= MAX_VALUE; j++) {
                BitSet jIndexes = map.get(j);

                if (jIndexes == null || jIndexes.cardinality() < referenceSupport) {
                    continue;
                }

                BitSet indexes = (BitSet) currentItemsetIndexes.clone();

                indexes.and(jIndexes);
                t = System.currentTimeMillis();
                if (indexes.cardinality() >= referenceSupport) {
                    ArrayList<Integer> items = currentItemset.getItems();
                    items.add(j);
                    Q.add(new Itemset(items, indexes));

                    if (indexes.cardinality() > referenceSupport) {
                        referenceSupport = clearQ();
                    }
                }
                //System.out.println("new itemset creation time: " + (System.currentTimeMillis() - t));
            }

        }

        referenceSupport = S.get(S.size() - 1).getSupport();

        while (!Q.isEmpty() && Q.peek().getSupport() == referenceSupport) {
            Itemset currentItemset = Q.poll();
            BitSet currentItemsetIndexes = currentItemset.getIndexes();
            
            S.add(currentItemset);

            for (int j = currentItemset.getMaxValueItem() + 1; j <= MAX_VALUE; j++) {
                BitSet jIndexes = map.get(j);

                if (jIndexes == null || jIndexes.cardinality() < referenceSupport) {
                    continue;
                }

                BitSet indexes = (BitSet) currentItemsetIndexes.clone();

                indexes.and(jIndexes);

                if (indexes.cardinality() >= referenceSupport) {
                    ArrayList<Integer> items = currentItemset.getItems();
                    items.add(j);
                    Q.add(new Itemset(items, indexes));

                    if (indexes.cardinality() > referenceSupport) {
                        referenceSupport = clearQ();
                    }
                }
            }
        }

        if (S.size() <= M) {
            System.out.println(S.size());
            for (Itemset itemset : S) {
                System.out.println(itemset);
            }
        }
        System.out.println("Total time: " + (System.currentTimeMillis() - startingTime));

    }

    private static void parseInputArgs(String[] args) {
        if (args.length != 3) {
            System.out.println("Please use\n $ java TopKFI datasetpath K M");
            System.exit(0);
        }

        path = args[0];
        try {
            K = Integer.parseInt(args[1]);
            M = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("K, M must be integers.");
            System.exit(0);
        }

        if (K < 0 || M < 0) {
            System.out.println("K, M must be positive integers.");
            System.exit(0);
        }
    }

    private static boolean parseDataset() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] transaction = line.split("\\s+");
                int item = 0;

                for (int i = 0; i < transaction.length; ++i) {
                    item = Integer.parseInt(transaction[i]);
                    
                    BitSet currentItemIndexes = map.get(item);

                    if (currentItemIndexes == null) {
                        currentItemIndexes = new BitSet();
                    }

                    currentItemIndexes.set(TRANSACTION_INDEX);
                    map.putIfAbsent(item, currentItemIndexes);
                }

                TRANSACTION_INDEX++;
                MAX_VALUE = Math.max(MAX_VALUE, item);
            }
            reader.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static int initializeQ() {
        PriorityQueue<Itemset> temp = new PriorityQueue<>(MAX_VALUE, Collections.reverseOrder());

        for (Entry<Integer, BitSet> entry : map.entrySet()) {
            temp.add(new Itemset(entry.getKey(), entry.getValue()));
        }

        int referenceSupport = 1;

        for (int i = 0; i < K && !temp.isEmpty(); i++) {
            if (i == K - 1) {
                referenceSupport = temp.peek().getSupport();
            }

            Q.add(temp.poll());
        }

        while (!temp.isEmpty() && temp.peek().getSupport() == referenceSupport) {
            Q.add(temp.poll());
        }

        return referenceSupport;
    }

    private static void clearMap(int referenceSupport) {
        long time = System.currentTimeMillis();
        ArrayList<Integer> keys = new ArrayList<>(map.size());
        
        for (Entry<Integer, BitSet> entry : map.entrySet()) {
            if (entry.getValue().size() < referenceSupport)
                keys.add(entry.getKey());
        }

        for (Integer key : keys) {
            map.remove(key);
        }

        System.out.println("Map clearing time: " + (System.currentTimeMillis() - time));
    }

    private static int clearQ() {
        int referenceSupport = 1;
        int j;

        PriorityQueue<Itemset> temp = new PriorityQueue<>(Q.size(), Collections.reverseOrder());

        if ((j = K - S.size()) > 0) {
            temp.addAll(Q);
            Q = new PriorityQueue<>(j, Collections.reverseOrder());

            for (int i = 0; i < j && !temp.isEmpty(); i++) {
                if (i == j - 1) {
                    referenceSupport = temp.peek().getSupport();
                }

                Q.add(temp.poll());
            }

        } else {
            referenceSupport = S.get(S.size() - 1).getSupport();
        }

        while (!temp.isEmpty() && temp.peek().getSupport() == referenceSupport) {
            Q.add(temp.poll());
        }

        return referenceSupport;
    }

}

class Itemset implements Comparable<Itemset> {
    private ArrayList<Integer> items = new ArrayList<>();
    private BitSet indexes;

    public Itemset(int item, BitSet indexes) {
        items.add(item);
        this.indexes = indexes;
    }

    public Itemset(ArrayList<Integer> items, BitSet indexes) {
        this.items = items;
        this.indexes = indexes;
    }

    public int getMaxValueItem() {
        return items.get(items.size() - 1);
    }

    public int getSupport() {
        return indexes.cardinality();
    }    

    public ArrayList<Integer> getItems() {
        return new ArrayList<>(items);
    }

    public BitSet getIndexes() {
        return (BitSet) indexes.clone();
    }

    public int compareTo(Itemset other) {
        return this.getSupport() - other.getSupport();
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        for (Integer item : items) {
            str.append(item + " ");
        }
        return str + "(" + getSupport() + ")";
    }
}