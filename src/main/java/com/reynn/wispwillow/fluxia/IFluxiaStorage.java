package com.reynn.wispwillow.fluxia;


public interface IFluxiaStorage {
    int insert(int amount, boolean simulate);   // returns accepted
    int extract(int amount, boolean simulate);  // returns extracted
    int amount();
    int capacity();
    default int canAcceptPerTick() { return Integer.MAX_VALUE; }
    default int canOutputPerTick() { return Integer.MAX_VALUE; }
}