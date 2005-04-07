/*
 * Created on Nov 25, 2004
 * 
 * file: TagResponsible.java
 */
package ddproto1.localagent;

/**
 * @author giuliano
 *
 */
public abstract class TagResponsible {

    private static final byte fullmask = (byte)255;

    /* gid + localId = 32 bits = int */
    private byte[] localId = new byte[3];

    
    public TagResponsible() {
    }
    
    protected synchronized int nextUID(){
        int nxt = encodeUUID();
        inc();
        return nxt;
    }
    
    private void inc() {
        int carrier = 1;

        for (int i = 0; i < 3; i++) {
            if (carrier == 0)
                continue;
            carrier = 0;
            if (localId[i] == fullmask) {
                localId[i] = 0;
                carrier = 1;
            } else
                localId[i]++;
        }
    }


    private int encodeUUID() {
        int uid = this.getGID();
        for (int i = 2; i >= 0; i--) {
            uid <<= 8;
            uid += localId[i];
        }

        return uid;
    }
    
    protected abstract byte getGID();
}
