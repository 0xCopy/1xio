
package org.jtorrent.ptop;

/**
 * a fast max heap, that uses twice as much memory as necessary
 * This is a Java port of the PriorityBitField.py module from BitTorrent 3.0.2
 *
 * @author Hunter Payne
 */
public class PriorityBitField {

    private int size;
    private int[] vals;
    private int p;

    public PriorityBitField(int size) {

	this.size = size;
	p = 1;
	
	while (p < size) p *= 2;
	vals = new int[p * 2];
	for (int i = 0; i < vals.length; i++) vals[i] = Integer.MAX_VALUE;
    }

    public boolean isEmpty() {

	return vals[1] == Integer.MAX_VALUE;
    }

    public int getFirst() {

	/*	System.out.print("getting first [");

	for (int i = 0; i < vals.length && i < 20; i++)
	    System.out.print(", " + vals[i]);

	    System.out.println("]");*/

	return vals[1];
    }

    public boolean insert(int index) {

	if (vals[index + p] > index) {

	    int i = index + p;
	    
	    while ((i > 0) && (vals[i] > index)) {

		vals[i] = index;
		i = i / 2;
	    }
	}

	return false;
    }

    public boolean remove(int index) {

	if (vals[index + p] == index) {

	    int i = index + p;
	    vals[i] = Integer.MAX_VALUE;

	    while (i > 1) {

		int n = i / 2;
		int m = Math.min(vals[n * 2], vals[n * 2 + 1]);
		
		if (vals[n] == m) return true;
		vals[n] = m;
		i = n;
	    }

	    return true;
	}

	return false;
    }

    public boolean contains(int index) {

	return vals[index + p] != Integer.MAX_VALUE;
    }
}
