package com.weeeedddd.orv.stigma;

@FunctionalInterface
public interface StigmaEffectLogic<P> {
    void apply(StigmaExecutionContext<P> context);
}
