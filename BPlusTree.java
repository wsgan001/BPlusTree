import java.util.ArrayList;
import java.util.List;
import javafx.util.Pair;
/*
    As specified in the project description, we can assume the key is of double type, and value of String type.
    Order of this B plus tree is specified from input file.
    Operations on this tree includes:
    0. initialization, which initialize the tree with specified order;
    1. insertion, which does not return anything;
    2. search by key, which returns all the values with this key in any order(return null if there is no such key);
    3. search by key range, which returns all key-value pairs within this key range(sorted by key in ascending order);
 */
public class BPlusTree {

    private  Node root;
    private int m;

    public BPlusTree(int order) {
        this.m = order;
        this.root = null;
    }

    public int treeOrder() {
        return m;
    }

    public Node treeRoot() {
        return root;
    }

    /*
      insert a key-value pair into the BPlusTree
      if sub-procedure returns null, then there is no overflow and child splitting
      if sub-procedure returns non-null, a new index node should be created
     */
    public void insertion(Double key, String value) {
        LeafNode newLeaf = new LeafNode(key, value);
        Pair<Double, Node> pair = new Pair<Double, Node>(key, newLeaf);
        
        // insert into an empty tree, this leaf node becomes a new root
        if(root == null || root.keys.size() == 0) {
            root = pair.getValue();
            return;
        }

        // initially newChildPair is null, and stays as null on return unless child is split
        Pair<Double, Node> newChildPair = insertionHelper(root, pair);

        // insertion does not split leaf node
        if(newChildPair == null) {
            return;
        } else {
            // splitting take place
            IndexNode newRoot = new IndexNode(newChildPair.getKey(), root, newChildPair.getValue());
            root = newRoot;
            return;
        }
    }

    private Pair<Double, Node> insertionHelper(Node node, Pair<Double, Node> pair) {
        Pair<Double, Node> newChildPair = null;
        if(!node.isLeafNode) {
            IndexNode curr = (IndexNode) node;
            int i = 0;

            // iterate node's keys to find i such that ith of keys <= pair's key value < (i+1)th of keys
            while(i < curr.keys.size()) {
                if(pair.getKey().compareTo(curr.keys.get(i)) < 0) {
                    break;
                }
                i++;
            }

            // recursively call the helper method until it hits a leaf node, then insert pair
            newChildPair = insertionHelper((Node)curr.children.get(i), pair);

            // the case that insertion does not split node
            if(newChildPair == null) {
                return null;
            } else {
                // the case that child node splits, need to insert newChildPair in node
                int j = 0;
                while (j < curr.keys.size()) {
                    if(newChildPair.getKey().compareTo(curr.keys.get(j)) < 0) {
                        break;
                    }
                    j++;
                }
                curr.insertSorted(newChildPair, j);

                // after insertion the newChildPair, need to check overflow status
                if(curr.keys.size() < m) {
                    // no overflow, return to the caller for this recursion
                    return null;
                }
                else{
                    newChildPair = splitIndexNode(curr);

                    // root was split, use newChildPair to create a new root and grow the tree height by 1
                    if(curr == root) {
                        // Create new node and make tree's root-node pointer point to newRoot
                        IndexNode newRoot = new IndexNode(newChildPair.getKey(), curr,
                                newChildPair.getValue());
                        root = newRoot;
                        //System.out.println("Root is index node, it is splited, tree height increase by 1");
                        return null;
                    }
                    return newChildPair;
                }
            }
        } else {
            LeafNode leaf = (LeafNode)node;
            LeafNode newLeaf = (LeafNode)pair.getValue();

            leaf.insertSorted(pair.getKey(), newLeaf.values.get(0).get(0));

            // the case that there is extra space for newLeaf
            if(leaf.keys.size() <= m) {
                return null;
            }
            else {
                // leaf is overflow after insertion
                //System.out.println("After insertion, size of leaf node is:" + leaf.keys.size());
                newChildPair = splitLeafNode(leaf);
                if(leaf == root) {
                    //System.out.println("New index node with key " + newChildPair.getKey());
                    //System.out.println("New index node with value " + ((LeafNode)newChildPair.getValue()).values.get(0).get(0));
                    IndexNode newRoot = new IndexNode(newChildPair.getKey(), leaf,newChildPair.getValue());
                    root = newRoot;
                    //System.out.println("Root is also leaf, it is splitted new root with key: " + root.keys.get(0));
                    return null;
                }
                return newChildPair;
            }
        }
    }

    // split a leaf node and return an pair consisting of splitting key and new leaf node
    private Pair<Double, Node> splitLeafNode(LeafNode leaf) {
        ArrayList<Double> newKeys = new ArrayList<>();
        List<List<String>> newValues = new ArrayList<>();

        // m/2 entries move to brand new node, leaf node has max of m children
        int start = (int)Math.ceil(m / 2);
        for(int i = start; i <= m; i++) {
            newKeys.add(leaf.keys.get(i));
            List<String> newBucket = leaf.values.get(i);
            newValues.add(newBucket);
        }

        // remove the the above entries from previous node
        for(int i = start; i <= m; i++) {
            leaf.keys.remove(leaf.keys.size()-1);
            leaf.values.remove(leaf.values.size()-1);
        }

        Double splitKey = newKeys.get(0);
        LeafNode rightNode = new LeafNode(newKeys, newValues);

        // add the new leaf node into doubly linked list

        if (leaf.nextSibling == null) {
            // leaf is the rightmost node
            leaf.nextSibling = rightNode;
            rightNode.preSibling = leaf;
        } else {
            LeafNode temp = leaf.nextSibling;
            leaf.nextSibling = rightNode;
            rightNode.nextSibling = temp;
            temp.preSibling = rightNode;
            rightNode.preSibling = leaf;
        }

        Pair<Double, Node> newChildPair = new Pair<Double, Node>(splitKey, rightNode);
        return newChildPair;
    }

    // split index node
    private Pair<Double, Node> splitIndexNode(IndexNode node) {
        ArrayList<Double> newKeys = new ArrayList<>();
        ArrayList<Node> newChildren = new ArrayList<>();

        // push the middle key up a level
        int splitIndex = (int)Math.ceil(m / 2) - 1;
        Double splitKey = node.keys.remove(splitIndex);

        // insert the leftmost child of new index node
        newChildren.add(node.children.remove(splitIndex + 1));

        // store the remaining right half of key and children into a new index node
        while(node.keys.size() > splitIndex) {
            newKeys.add(node.keys.remove(splitIndex));
            newChildren.add(node.children.remove(splitIndex + 1));
        }

        IndexNode rightNode = new IndexNode(newKeys, newChildren);
        Pair<Double, Node> newChildPair = new Pair<Double, Node>(splitKey, rightNode);
        return newChildPair;
    }

    /*
    search a key and return the corresponding value
    if the key does not exist, return null
    */
    public List<String> search(Double key) {
        // return null if tree is empty or the given key does not exist
        if(key == null || root == null) {
            return null;
        }
        // find the leaf node that key is pointing to
        LeafNode leaf = (LeafNode)searchHelper(root, key);

        //  iterate keys of the leaf to find the correct bucket
        for(int i=0; i<leaf.keys.size(); i++) {
            if(key.compareTo(leaf.keys.get(i)) == 0) {
                return leaf.values.get(i);
            }
        }
        return null;
    }

    private Node searchHelper(Node node, Double key) {
        if(node.isLeafNode) {
            return node;
        } else {
            IndexNode indexNode = (IndexNode)node;
            if (key.compareTo(indexNode.keys.get(0)) < 0) {
                // inserting key is the smallest
                return searchHelper(indexNode.children.get(0), key);
            } else if (key.compareTo(indexNode.keys.get(indexNode.keys.size() - 1)) >= 0) {
                // inserting key is greater than or equal to the largest key
                return searchHelper(indexNode.children.get(indexNode.children.size() - 1), key);
            } else {
                int i = 0;
                for (i = 0; i < indexNode.keys.size() - 1; i++) {
                    if (key.compareTo(indexNode.keys.get(i)) >= 0 && key.compareTo(indexNode.keys.get(i + 1)) < 0) {
                        // find a potential indexNode, then recursively find in its children
                        break;
                    }
                }
                return searchHelper(indexNode.children.get(i + 1), key);
            }
        }
    }
     

    public List<Pair<Double, String>> searchRange(Double low, Double high) {
        if(low == null || high == null || root == null) {
            return null;
        }
        if (low > high) {
            return null;
        }
        // find the leaf node that key is pointing to
        LeafNode leaf = (LeafNode)searchHelper(root, low);
        if (leaf == null) {
            return null;
        }
        // then use the sibling pointer to find all key-value paris
        List<Pair<Double, String>> result = new ArrayList<>();

        do {
            for(int i=0; i<leaf.keys.size(); i++) {
                Double cmpKey = leaf.keys.get(i);
                if(low.compareTo(cmpKey) <= 0 && high.compareTo(cmpKey) >= 0) {
                    Double key = leaf.keys.get(i);
                    List<String> valueList = leaf.values.get(i);
                    for (String val : valueList) {
                        Pair<Double, String> pair = new Pair<>(key, val);
                        result.add(pair);
                    }
                }
            }
            leaf = leaf.nextSibling;
        } while (leaf != null && leaf.keys.get(0) <= high);
        return result;
    }
}
