/*
 * Created on Aug 22, 2005
 * 
 * file: IntegerIntervalImpl.java
 */
package ddproto1.configurator;

public class IntegerIntervalImpl implements IIntegerInterval {

    private int min;
    private int max;

    public IntegerIntervalImpl(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }
}
