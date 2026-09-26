package dev.mw19.core;

/**
 * Wraps every call that crosses from Minecraft into our code so an exception can never take the game down.
 * VirtualMachineErrors (OOM, StackOverflow) are rethrown: swallowing those would only hide a dying JVM.
 */
public final class Guard {
    private Guard() {}

    public static boolean run(String what, Runnable r) {
        try {
            r.run();
            return true;
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("hook " + what + " failed", t);
            return false;
        }
    }
}
