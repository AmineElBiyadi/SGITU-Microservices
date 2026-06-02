package com.ensate.billetterie.context;

public final class RequestContextHolder {

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    private RequestContextHolder() {}

    public static void set(RequestContext context) {
        CONTEXT.set(context);
    }

    public static RequestContext get() {
        RequestContext ctx = CONTEXT.get();
        if (ctx == null) {
            throw new IllegalStateException("No RequestContext bound to current thread. Is this being called outside a request?");
        }
        return ctx;
    }

    public static boolean isPresent() {
        return CONTEXT.get() != null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
