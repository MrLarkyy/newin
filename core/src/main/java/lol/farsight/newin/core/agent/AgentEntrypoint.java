package lol.farsight.newin.core.agent;

import org.jetbrains.annotations.NotNull;

import java.lang.instrument.Instrumentation;

public final class AgentEntrypoint {
    public static void agentmain(final @NotNull String args, final @NotNull Instrumentation inst) {
        InstrumentationHolder.instrumentation = inst;
    }
}
