package communication;

import java.nio.channels.SocketChannel;

public class Header {
    private OPS op;

    public Header(OPS op) {
        this.op = op;
    }

    public Header(SocketChannel c){
        this.op = OPS.OK;
    }

    public OPS getOp() {
        return op;
    }

    public void setOp(OPS op) {
        this.op = op;
    }
}
