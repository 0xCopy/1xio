
package org.jtorrent.ptop;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Random;

/**
 * This class is a direct port of the PiecePicker.py file in BitTorrent 3.2.1
 * I have only a vague idea of how it work.  It is fairly obfuscated code.
 * It implements a rarest first ordering of which pieces to download first.
 *
 * @author Hunter Payne
 */
public class PiecePicker {

    private int numPieces;
    private ArrayList interests;
    private ArrayList numInterests;
    private ArrayList interestPos;
    private ArrayList fixed;
    private boolean gotAny;

    /**
     * Makes a new piece piece
     *
     * @param numPieces -- number of pieces in this download
     */
    public PiecePicker(int numPieces) {

	this.numPieces = numPieces;
	interests = new ArrayList(numPieces);
	ArrayList list = new ArrayList(numPieces);
	for (int i = 0; i < numPieces; i++) list.add(new Integer(i));
	interests.add(list);

	interestPos = new ArrayList(numPieces);
	for (int i = 0; i < numPieces; i++) interestPos.add(new Integer(i));
	
	numInterests = new ArrayList(numPieces);
	for (int i = 0; i < numPieces; i++) interestPos.add(new Integer(0));
	
	fixed = new ArrayList();
	gotAny = false;
    }

    private static void shuffle(ArrayList ret) {

	Random r = new Random();
	int loop = ret.size() * ret.size();

	for (int i = 0; i < loop; i++) {

	    int index1 = r.nextInt(ret.size());
	    int index2 = r.nextInt(ret.size());
	    Object temp = ret.get(index1);
	    ret.set(index1, ret.get(index2));
	    ret.set(index2, temp);
	}
    }

    public class SinglePicker implements Iterator {

	private int numInterest;
	private int numDone;
	private int fixedPos;

	public SinglePicker() {

	    numInterest = 1;
	    numDone = 0;
	    fixedPos = 0;
	}

	public boolean hasNext() {

	    if (fixedPos < fixed.size()) return true;
	    return (numInterest < interests.size());
	}

	public Object next() {

	    if (fixedPos < fixed.size()) {

		fixedPos++;
		return fixed.get(fixedPos - 1);
	    }

	    if (numInterest < interests.size()) {

		ArrayList localInterests = (ArrayList)interests.get(numInterest);
		
		while (localInterests.size() <= numDone) {
		    
		    numInterest++;
		    numDone = 0;
		    localInterests = (ArrayList)interests.get(numInterest);
		}

		numDone++;
		int y = interests.size() - numDone;
		Random r = new Random();
		int x = r.nextInt(y + 1);
		Object last = localInterests.get(x);
		localInterests.set(x, localInterests.get(y));
		localInterests.set(y, last);
		interestPos.set(x, localInterests.get(x));
		interestPos.set(y, localInterests.get(y));
		return last;
	    } 

	    return null;
	}

	public void remove() {

	    throw new RuntimeException("remove() not implemented");
	}
    }

    public class RandomPicker implements Iterator {

	private int fixedPos;
	private ArrayList l;

	public RandomPicker() {

	    fixedPos = 0;
	    l = null;
	}

	public boolean hasNext() {

	    if (fixedPos < fixed.size()) return true;

	    if (l == null) {

		l = new ArrayList();
		
		for (int i = 1; i < interests.size(); i++) l.add(new Integer(i));
		shuffle(l);
	    }

	    return (l.size() > 0);
	}

	public Object next() {

	    if (fixedPos < fixed.size()) {

		fixedPos++;
		return fixed.get(fixedPos - 1);
	    }

	    if (l == null) {

		l = new ArrayList();
		
		for (int i = 1; i < interests.size(); i++) l.add(new Integer(i));
		shuffle(l);
	    }

	    if (l.size() > 0) return l.remove(l.size() - 1);
	    return null;
	}

	public void remove() {

	    throw new RuntimeException("remove() not implemented");
	}
    }

    /**
     * notifies this object that one of this peer's neighbors has a new piece available for 
     * download
     *
     * @param index -- piece index which was downloaded by the neighbor peer
     */
    public void gotHave(int index) {

	if (numInterests.get(index) == null) return;
	ArrayList localInterests = 
	    (ArrayList)interests.get(((Integer)numInterests.get(index)).intValue());
	int pos = ((Integer)interestPos.get(index)).intValue();
	localInterests.set(pos, localInterests.get(localInterests.size() - 1));
	interestPos.set(((Integer)localInterests.get(localInterests.size() - 1)).intValue(), 
			new Integer(pos));
	localInterests.remove(localInterests.size() - 1);
	numInterests.set(index, new Integer(((Integer)numInterests.get(index)).intValue() + 1));
	
	if (interests.size() == ((Integer)numInterests.get(index)).intValue()) 
	    interests.add(new ArrayList());

	localInterests = (ArrayList)interests.get(((Integer)numInterests.get(index)).intValue());
	interestPos.set(index, new Integer(interests.size()));
	localInterests.add(new Integer(index));
    }

    /**
     * notifies this object that one of this peer's neighbors has exited and this piece is no longer
     * available because the peer is no longer available.
     *
     * @param index -- piece index which is effected by the disappearance of a neighboring peer
     */
    public void lostHave(int index) {

	if (numInterests.get(index) == null) return;
	ArrayList localInterests = 
	    (ArrayList)interests.get(((Integer)numInterests.get(index)).intValue());
	int pos = ((Integer)interestPos.get(index)).intValue();
	localInterests.set(pos, localInterests.get(localInterests.size() - 1));
	interestPos.set(((Integer)localInterests.get(localInterests.size() - 1)).intValue(), 
			new Integer(pos));
	localInterests.remove(localInterests.size() - 1);
	numInterests.set(index, new Integer(((Integer)numInterests.get(index)).intValue() - 1));
	localInterests = (ArrayList)interests.get(((Integer)numInterests.get(index)).intValue());
	interestPos.set(index, new Integer(interests.size()));
	localInterests.add(new Integer(index));
    }

    private void cameIn(int index) {

	if (numInterests.get(index) != null) {

	    ArrayList localInterests = 
		(ArrayList)interests.get(((Integer)numInterests.get(index)).intValue());
	    localInterests.set(((Integer)interestPos.get(index)).intValue(), 
			       interests.get(interests.size() - 1));
	    interestPos.set(((Integer)localInterests.get(localInterests.size() - 1)).intValue(),
			    interestPos.get(index));
	    
	    interests.remove(interests.size() - 1);
	    numInterests.set(index, null);
	    fixed.add(new Integer(index));
	}
    }

    /**
     * notifies this object that this peer has downloaded this piece and so doesn't need to 
     * know about the availability of the piece on neighboring peers
     *
     * @param index -- piece index which was downloaded by this peer
     */
    public void complete(int index) {

	gotAny = true;
	cameIn(index);
	fixed.remove(index);
    }

    /**
     * gets an iterator object which can list the piece indicies in rarest first order
     */
    public Iterator iterator() {

	if (gotAny) return new SinglePicker();
	return new RandomPicker();
    }
}
