package hideftvads.radixtree;

import ds.tree.DuplicateKeyException;
import ds.tree.RadixTreeImpl;
import javolution.text.Text;

import java.util.ArrayList;

 import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest
        extends TestCase {
    RadixTreeImpl<Text> trie;
    static final Text ABCE = Text.valueOf("abce");
    static final Text ABCD = Text.valueOf("abcd");
    static final Text APPLE = Text.valueOf("apple");
    static final Text BAT = Text.valueOf("bat");
    static final Text APE = Text.valueOf("ape");
    static final Text BATH = Text.valueOf("bath");
    static final Text BANANA = Text.valueOf("banana");
    static final Text APPLEPIE = Text.valueOf("applepie");
    static final Text APPLECRISP = Text.valueOf("applecrisp");
    static final Text APPLE2 = Text.valueOf("apple2");
    static final Text XBOX_360 = Text.valueOf("xbox 360");
    static final Text XBOX = Text.valueOf("xbox");
    static final Text XBOX_360_GAMES = Text.valueOf("xbox 360 games");
    static final Text XBOX_GAMES = Text.valueOf("xbox games");
    static final Text XBOX_XBOX_360 = Text.valueOf("xbox xbox 360");
    static final Text XBOX_XBOX = Text.valueOf("xbox xbox");
    static final Text XBOX_360_XBOX_GAMES = Text.valueOf("xbox 360 xbox games");
    static final Text XBOX_GAMES_360 = Text.valueOf("xbox games 360");
    static final Text XBOX_360_360 = Text.valueOf("xbox 360 360");
    static final Text XBOX_360_XBOX_360 = Text.valueOf("xbox 360 xbox 360");
    static final Text XBOX_360_GAMES_360 = Text.valueOf("360 xbox games 360");
    static final Text XBOX_XBOX_361 = Text.valueOf("xbox xbox 361");
    static final Text APPLESHACK = Text.valueOf("appleshack");
    static final Text BALL = Text.valueOf("ball");
    static final Text AP = Text.valueOf("ap");
    static final Text APPLETREE = Text.valueOf("appletree");
    static final Text APPLESHACKCREAM = Text.valueOf("appleshackcream");
    static final Text APP = Text.valueOf("app");
    static final Text APPL = Text.valueOf("appl");
    static final Text ABE = Text.valueOf("abe");
    static final Text ABD = Text.valueOf("abd");
    static final Text AB = Text.valueOf("ab");
    static final Text A = Text.valueOf("a");
    static final Text ABC = Text.valueOf("abc");

    {
        trie = new RadixTreeImpl<Text>();
    }


    public void testSearchForPartialParentAndLeafKeyWhenOverlapExists() {
        trie.insert(ABCD, ABCD);
        trie.insert(ABCE, ABCE);

        assertEquals(0, trie.searchPrefix(ABE, 10).size());
        assertEquals(0, trie.searchPrefix(ABD, 10).size());
    }


    public void testSearchForLeafNodesWhenOverlapExists() {
        trie.insert(ABCD, ABCD);
        trie.insert(ABCE, ABCE);

        assertEquals(1, trie.searchPrefix(ABCD, 10).size());
        assertEquals(1, trie.searchPrefix(ABCE, 10).size());
    }


    public void testSearchForTextSmallerThanSharedParentWhenOverlapExists() {
        trie.insert(ABCD, ABCD);
        trie.insert(ABCE, ABCE);

        assertEquals(2, trie.searchPrefix(AB, 10).size());
        assertEquals(2, trie.searchPrefix(A, 10).size());
    }

    //
    public void testSearchForTextEqualToSharedParentWhenOverlapExists() {
        trie.insert(ABCD, ABCD);
        trie.insert(ABCE, ABCE);

        assertEquals(2, trie.searchPrefix(ABC, 10).size());
    }


    public void testInsert() {
        trie.insert(APPLE, APPLE);
        trie.insert(BAT, BAT);
        trie.insert(APE, APE);
        trie.insert(BATH, BATH);
        trie.insert(BANANA, BANANA);

        assertEquals(trie.find(APPLE), APPLE);
        assertEquals(trie.find(BAT), BAT);
        assertEquals(trie.find(APE), APE);
        assertEquals(trie.find(BATH), BATH);
        assertEquals(trie.find(BANANA), BANANA);
    }


    public void testInsertExistingUnrealNodeConvertsItToReal() {
        trie.insert(APPLEPIE, APPLEPIE);
        trie.insert(APPLECRISP, APPLECRISP);

        assertFalse(trie.contains(APPLE));

        trie.insert(APPLE, APPLE);

        assertTrue(trie.contains(APPLE));
    }


    public void testDuplicatesNotAllowed() {
        RadixTreeImpl<Text> trie = new RadixTreeImpl<Text>();

        trie.insert(APPLE, APPLE);

        try {
            trie.insert(APPLE, APPLE2);
            fail("Duplicate should not have been allowed");
        } catch (DuplicateKeyException e) {
            assertEquals("Duplicate key: 'apple'", e.getMessage());
        }
    }


    public void testInsertWithRepeatingPatternsInKey() {
        trie.insert(XBOX_360, XBOX_360);
        trie.insert(XBOX, XBOX);
        trie.insert(XBOX_360_GAMES, XBOX_360_GAMES);
        trie.insert(XBOX_GAMES, XBOX_GAMES);
        trie.insert(XBOX_XBOX_360, XBOX_XBOX_360);
        trie.insert(XBOX_XBOX, XBOX_XBOX);
        trie.insert(XBOX_360_XBOX_GAMES, XBOX_360_XBOX_GAMES);
        trie.insert(XBOX_GAMES_360, XBOX_GAMES_360);
        trie.insert(XBOX_360_360, XBOX_360_360);
        trie.insert(XBOX_360_XBOX_360, XBOX_360_XBOX_360);
        trie.insert(XBOX_360_GAMES_360, XBOX_360_GAMES_360);
        trie.insert(XBOX_XBOX_361, XBOX_XBOX_361);

        assertEquals(12, trie.getSize());

        trie.display();

    }


    public void testDeleteNodeWithNoChildren() {
        RadixTreeImpl<Text> trie = new RadixTreeImpl<Text>();
        trie.insert(APPLE, APPLE);
        assertTrue(trie.delete(APPLE));
    }


    public void testDeleteNodeWithOneChild() {
        RadixTreeImpl<Text> trie = new RadixTreeImpl<Text>();
        trie.insert(APPLE, APPLE);
        trie.insert(APPLEPIE, APPLEPIE);
        assertTrue(trie.delete(APPLE));
        assertTrue(trie.contains(APPLEPIE));
        assertFalse(trie.contains(APPLE));
    }


    public void testDeleteNodeWithMultipleChildren() {
        RadixTreeImpl<Text> trie = new RadixTreeImpl<Text>();
        trie.insert(APPLE, APPLE);
        trie.insert(APPLEPIE, APPLEPIE);
        trie.insert(APPLECRISP, APPLECRISP);
        assertTrue(trie.delete(APPLE));
        assertTrue(trie.contains(APPLEPIE));
        assertTrue(trie.contains(APPLECRISP));
        assertFalse(trie.contains(APPLE));
    }


    public void testCantDeleteSomethingThatDoesntExist() {
        RadixTreeImpl<Text> trie = new RadixTreeImpl<Text>();
        assertFalse(trie.delete(APPLE));
    }


    public void testCantDeleteSomethingThatWasAlreadyDeleted() {
        RadixTreeImpl<Text> trie = new RadixTreeImpl<Text>();
        trie.insert(APPLE, APPLE);
        trie.delete(APPLE);
        assertFalse(trie.delete(APPLE));
    }


    public void testChildrenNotAffectedWhenOneIsDeleted() {
        RadixTreeImpl<Text> trie = new RadixTreeImpl<Text>();
        trie.insert(APPLE, APPLE);
        trie.insert(APPLESHACK, APPLESHACK);
        trie.insert(APPLEPIE, APPLEPIE);
        trie.insert(APE, APE);

        trie.delete(APPLE);

        assertTrue(trie.contains(APPLESHACK));
        assertTrue(trie.contains(APPLEPIE));
        assertTrue(trie.contains(APE));
        assertFalse(trie.contains(APPLE));
    }


    public void testSiblingsNotAffectedWhenOneIsDeleted() {
        RadixTreeImpl<Text> trie = new RadixTreeImpl<Text>();
        trie.insert(APPLE, APPLE);
        trie.insert(BALL, BALL);

        trie.delete(APPLE);

        assertTrue(trie.contains(BALL));
    }


    public void testCantDeleteUnrealNode() {
        RadixTreeImpl<Text> trie = new RadixTreeImpl<Text>();
        trie.insert(APPLE, APPLE);
        trie.insert(APE, APE);

        assertFalse(trie.delete(AP));
    }


    public void testCantFindRootNode() {
        assertNull(trie.find(Text.EMPTY));
    }


    public void testFindSimpleInsert() {
        trie.insert(APPLE, APPLE);
        assertNotNull(trie.find(APPLE));
    }


    public void testContainsSimpleInsert() {
        trie.insert(APPLE, APPLE);
        assertTrue(trie.contains(APPLE));
    }


    public void testFindChildInsert() {
        trie.insert(APPLE, APPLE);
        trie.insert(APE, APE);
        trie.insert(APPLETREE, APPLETREE);
        trie.insert(APPLESHACKCREAM, APPLESHACKCREAM);
        assertNotNull(trie.find(APPLETREE));
        assertNotNull(trie.find(APPLESHACKCREAM));
        assertNotNull(trie.contains(APE));
    }


    public void testContainsChildInsert() {
        trie.insert(APPLE, APPLE);
        trie.insert(APE, APE);
        trie.insert(APPLETREE, APPLETREE);
        trie.insert(APPLESHACKCREAM, APPLESHACKCREAM);
        assertTrue(trie.contains(APPLETREE));
        assertTrue(trie.contains(APPLESHACKCREAM));
        assertTrue(trie.contains(APE));
    }


    public void testCantFindNonexistantNode() {
        assertNull(trie.find(APPLE));
    }


    public void testDoesntContainNonexistantNode() {
        assertFalse(trie.contains(APPLE));
    }


    public void testCantFindUnrealNode() {
        trie.insert(APPLE, APPLE);
        trie.insert(APE, APE);
        assertNull(trie.find(AP));
    }


    public void testDoesntContainUnrealNode() {
        trie.insert(APPLE, APPLE);
        trie.insert(APE, APE);
        assertFalse(trie.contains(AP));
    }


    public void testSearchPrefix_LimitGreaterThanPossibleResults() {
        trie.insert(APPLE, APPLE);
        trie.insert(APPLESHACK, APPLESHACK);
        trie.insert(APPLESHACKCREAM, APPLESHACKCREAM);
        trie.insert(APPLEPIE, APPLEPIE);
        trie.insert(APE, APE);

        ArrayList<Text> result = trie.searchPrefix(APP, 10);
        assertEquals(4, result.size());

        assertTrue(result.contains(APPLESHACK));
        assertTrue(result.contains(APPLESHACKCREAM));
        assertTrue(result.contains(APPLEPIE));
        assertTrue(result.contains(APPLE));
    }


    public void testSearchPrefix_LimitLessThanPossibleResults() {
        trie.insert(APPLE, APPLE);
        trie.insert(APPLESHACK, APPLESHACK);
        trie.insert(APPLESHACKCREAM, APPLESHACKCREAM);
        trie.insert(APPLEPIE, APPLEPIE);
        trie.insert(APE, APE);

        ArrayList<Text> result = trie.searchPrefix(APPL, 3);
        assertEquals(3, result.size());

        assertTrue(result.contains(APPLESHACK));
        assertTrue(result.contains(APPLEPIE));
        assertTrue(result.contains(APPLE));
    }


    public void testGetSize() {
        trie.insert(APPLE, APPLE);
        trie.insert(APPLESHACK, APPLESHACK);
        trie.insert(APPLESHACKCREAM, APPLESHACKCREAM);
        trie.insert(APPLEPIE, APPLEPIE);
        trie.insert(APE, APE);

        assertTrue(trie.getSize() == 5);
    }


    public void testDeleteReducesSize() {
        trie.insert(APPLE, APPLE);
        trie.insert(APPLESHACK, APPLESHACK);

        trie.delete(APPLESHACK);

        assertTrue(trie.getSize() == 1);
    }

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        assertTrue(true);
    }
}
