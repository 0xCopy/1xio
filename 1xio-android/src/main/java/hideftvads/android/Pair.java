package hideftvads.android;

import alg.*;

import java.util.Arrays;


/**
 * User: jim
 * Date: May 13, 2009
 * Time: 7:11:18 AM
 */
public class Pair<$1, $2>   implements Tuple{
    Object[] v;

            static
{
       // XSTREAM.aliasType("pair", Pair.class);
    }

    public Pair(Object... v) {
        this.v = v;
    }

    public Pair($1 $1, $2 $2) {
        v=new Object[]{$1,$2};
    }

    public $1 $1() {
        return ($1) v[0];

    }

    public $2 $2() {
        return ($2) v[1];

    }

    public Pair<$1, $2> $1($1 $1) {

        final $1 $11 = v.length > 1 ?
                ($1) (v[0] = $1)
                : ($1) (this.v = new Object[]
                {$1, $2()})[0];
        return this;

    }

    public Pair<$1, $2> $2($2 $2) {

        final $2 $21 = v.length > 1 ?
                ($2) (v[1] = $2)
                : ($2) (this.v = new Object[]
                {$1(), $2})[1];
        return this;

    }

    public String toString(){
        return Arrays.toString(v);}
}