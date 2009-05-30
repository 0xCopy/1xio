package alg;


/**
 * Copyright hideftvads.com 2009 all rights reserved.
 * <p/>
 * User: jim
 * Date: May 15, 2009
 * Time: 8:21:44 PM
 */
public class Triple<$1, $2, $3> implements Tuple{
    Object[] v;

    static {
        XSTREAM.aliasType("triple", Triple.class);

     }

    public Triple(Object... v) {
        this.v = v;
    }

    public $1 $1() {
        return ($1) v[0];

    }

    public $2 $2() {
        return ($2) v[1];

    }

    public $3 $3() {
        return ($3) v[2];
    }

    public Triple $1($1 $1) {

        final $1 $11 = v.length > 1 ?
                ($1) (v[0] = $1)
                : ($1) (this.v = new Object[]
                {$1, $2(), $3()})[0];
        return    this;
    }

    public Triple $2($2 $2) {
        final $2 $21 = v.length > 1 ?
                ($2) (v[1] = $2)
                : ($2) (this.v = new Object[]
                {$1(), $2, $3()})[1];

        return     this;

    }

    public Triple $3($3 $3) {
        final $3 $31 = v.length > 2 ?
                ($3) (v[2] = $3)
                : ($3) (this.v = new Object[]
                {$1(), $2(), $3})[2];
        return     this; 
    }
}
