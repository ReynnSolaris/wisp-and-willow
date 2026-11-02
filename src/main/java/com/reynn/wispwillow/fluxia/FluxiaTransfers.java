package com.reynn.wispwillow.fluxia;

public final class FluxiaTransfers {
    private FluxiaTransfers(){}

    /** Attempts to move up to {@code wanted} from src->dst, honoring both per-tick caps; returns delivered amount. */
    public static int move(IFluxiaStorage src, IFluxiaNode var, int wanted, boolean simulate){
        if (wanted <= 0) return 0;
        int take = Math.min(wanted, Math.min(src.canOutputPerTick(), src.amount()));
        IFluxiaStorage dst = var.getFluxStorage();
        int give = Math.min(take, dst.canAcceptPerTick());
        if (give <= 0) return 0;
        if (simulate) return Math.min(give, dst.insert(give, true));
        int extracted = src.extract(give, false);
        if (extracted <= 0) return 0;
        int accepted = dst.insert(extracted, false);
        if (accepted < extracted) src.insert(extracted - accepted, false); // put back excess (shouldn’t happen)
        return accepted;
    }

    public static int move(IFluxiaStorage src, IFluxiaStorage dst, int wanted, boolean simulate){
        if (wanted <= 0) return 0;
        int take = Math.min(wanted, Math.min(src.canOutputPerTick(), src.amount()));
        int give = Math.min(take, dst.canAcceptPerTick());
        if (give <= 0) return 0;
        if (simulate) return Math.min(give, dst.insert(give, true));
        int extracted = src.extract(give, false);
        if (extracted <= 0) return 0;
        int accepted = dst.insert(extracted, false);
        if (accepted < extracted) src.insert(extracted - accepted, false); // put back excess (shouldn’t happen)
        return accepted;
    }
}
