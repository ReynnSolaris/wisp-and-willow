package com.reynn.wispwillow.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class FluxSpark extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected FluxSpark(ClientLevel lvl, double x, double y, double z,
                        double vx, double vy, double vz, SpriteSet sprites) {
        super(lvl, x, y, z, vx, vy, vz);
        this.sprites = sprites;

        // ---- vanilla FlameParticle-style motion setup ----
        this.xd = this.xd * 0.01D + vx;
        this.yd = this.yd * 0.01D + vy;
        this.zd = this.zd * 0.01D + vz;

        this.x += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
        this.y += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
        this.z += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;

        this.quadSize *= this.random.nextFloat() * 0.5F + 0.2F;
        this.lifetime = (int)(8.0D / (this.random.nextDouble() * 0.8D + 0.2D));

        // torch-ish size
        this.setSize(0.5F, 0.5F);

        // Tint: purple-ish flame instead of orange.
        // If you want default torch color, just use 1,1,1.
        this.rCol = 0.85f;
        this.gCol = 0.55f;
        this.bCol = 1.0f;

        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            // same slow-down as FlameParticle
            this.xd *= 0.96D;
            this.yd *= 0.96D;
            this.zd *= 0.96D;
        }
    }

    // fade exactly like vanilla flame: 1 -> ~0 over lifetime
    @Override
    public void render(VertexConsumer vc, Camera cam, float partialTicks) {
        float f = ((float)this.age + partialTicks) / (float)this.lifetime;
        this.alpha = 1.0F - f * f * 0.5F;
        super.render(vc, cam, partialTicks);
    }

    // shrink with age just like the torch flame
    @Override
    public float getQuadSize(float partialTicks) {
        float f = ((float)this.age + partialTicks) / (float)this.lifetime;
        return this.quadSize * (1.0F - f * f * 0.5F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        // vanilla flame uses LIT, not translucent
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites){ this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel lvl,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new FluxSpark(lvl, x, y, z, vx, vy, vz, sprites);
        }
    }
}