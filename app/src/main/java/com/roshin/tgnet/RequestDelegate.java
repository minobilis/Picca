package com.roshin.tgnet;

public interface RequestDelegate {
    void run(TLObject response, TLRPC.TL_error error);
}
