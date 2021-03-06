package com.resourcefulbees.resourcefulbees.client.render.entity.layers;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.resourcefulbees.resourcefulbees.ResourcefulBees;
import com.resourcefulbees.resourcefulbees.api.beedata.ColorData;
import com.resourcefulbees.resourcefulbees.client.render.entity.CustomBeeRenderer;
import com.resourcefulbees.resourcefulbees.client.render.entity.models.CustomBeeModel;
import com.resourcefulbees.resourcefulbees.entity.passive.CustomBeeEntity;
import com.resourcefulbees.resourcefulbees.lib.BeeConstants;
import com.resourcefulbees.resourcefulbees.lib.ModelTypes;
import com.resourcefulbees.resourcefulbees.utils.color.RainbowColor;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class BeeLayer extends LayerRenderer<CustomBeeEntity, CustomBeeModel<CustomBeeEntity>> {

    private final boolean isEmissive;
    private final int glowingPulse;
    private final boolean isEnchanted;
    private final CustomBeeModel<CustomBeeEntity> additionModel;
    private final ResourceLocation layerTexture;
    private final ResourceLocation angerLayerTexture;
    private float[] color;
    private final boolean isRainbowBee;

    public BeeLayer(IEntityRenderer<CustomBeeEntity, CustomBeeModel<CustomBeeEntity>> renderer, CustomBeeRenderer.LayerType layerType, ModelTypes addition, ColorData colorData) {
        super(renderer);
        this.additionModel = addition == ModelTypes.DEFAULT ? null : new CustomBeeModel<>(addition);
        this.isRainbowBee = colorData.isRainbowBee();
        this.isEnchanted = colorData.isEnchanted();
        this.glowingPulse = colorData.getGlowingPulse();

        switch (layerType) {
            case PRIMARY:
                this.isEmissive = false;
                this.layerTexture = new ResourceLocation(ResourcefulBees.MOD_ID, BeeConstants.ENTITY_TEXTURES_DIR + colorData.getPrimaryLayerTexture() + ".png");
                this.angerLayerTexture = new ResourceLocation(ResourcefulBees.MOD_ID, BeeConstants.ENTITY_TEXTURES_DIR + colorData.getPrimaryLayerTexture() + "_angry.png");
                this.color = isRainbowBee ? RainbowColor.getColorFloats() : colorData.getPrimaryColorFloats();
                break;
            case SECONDARY:
                this.isEmissive = false;
                this.layerTexture = new ResourceLocation(ResourcefulBees.MOD_ID, BeeConstants.ENTITY_TEXTURES_DIR + colorData.getSecondaryLayerTexture() + ".png");
                this.angerLayerTexture = new ResourceLocation(ResourcefulBees.MOD_ID, BeeConstants.ENTITY_TEXTURES_DIR + colorData.getSecondaryLayerTexture() + "_angry.png");
                this.color = isRainbowBee ? RainbowColor.getColorFloats() : colorData.getSecondaryColorFloats();
                break;
            case EMISSIVE:
                this.isEmissive = true;
                this.layerTexture = new ResourceLocation(ResourcefulBees.MOD_ID, BeeConstants.ENTITY_TEXTURES_DIR + colorData.getEmissiveLayerTexture() + ".png");
                this.angerLayerTexture = new ResourceLocation(ResourcefulBees.MOD_ID, BeeConstants.ENTITY_TEXTURES_DIR + colorData.getEmissiveLayerTexture() + "_angry.png");
                this.color = isRainbowBee ? RainbowColor.getColorFloats() : colorData.getGlowColorFloats();
                break;
            default:
                throw new IllegalStateException("You dun screwed up, did you add a new layer?");
        }
    }

    @Override
    public void render(@Nonnull MatrixStack matrixStackIn, @Nonnull IRenderTypeBuffer bufferIn, int packedLightIn, @Nonnull CustomBeeEntity customBeeEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (isRainbowBee) color = RainbowColor.getColorFloats();
        ResourceLocation texture = customBeeEntity.hasAngerTime() && angerLayerTexture != null ? angerLayerTexture : layerTexture;

        if (additionModel != null) {
            this.getEntityModel().setModelAttributes(additionModel);
            additionModel.setLivingAnimations(customBeeEntity, limbSwing, limbSwingAmount, partialTicks);
            additionModel.setAngles(customBeeEntity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        }

        if (isEmissive) {
            if (isEnchanted) {
                this.getEntityModel().render(matrixStackIn, bufferIn.getBuffer(RenderType.getEntityGlint()), packedLightIn, OverlayTexture.DEFAULT_UV, 0.0F, 0.0F, 0.0F, 0.0F);
                if (additionModel != null) {
                    additionModel.render(matrixStackIn, bufferIn.getBuffer(RenderType.getEntityGlint()), packedLightIn, LivingRenderer.getOverlay(customBeeEntity, 0.0F), 0.0F, 0.0F, 0.0F, 0.0F);
                }
            } else {
                IVertexBuilder ivertexbuilder = bufferIn.getBuffer(RenderType.getEyes(texture));
                if (glowingPulse == 0 || customBeeEntity.ticksExisted / 5 % glowingPulse == 0) {
                    this.getEntityModel().render(matrixStackIn, ivertexbuilder, 15728640, OverlayTexture.DEFAULT_UV, color[0], color[1], color[2], 1.0F);
                    if (additionModel != null) {
                        additionModel.render(matrixStackIn, ivertexbuilder, 15728640, LivingRenderer.getOverlay(customBeeEntity, 0.0F), color[0], color[1], color[2], 1.0F);
                    }
                }
            }
        } else {
            renderModel(this.getEntityModel(), texture, matrixStackIn, bufferIn, packedLightIn, customBeeEntity, color[0], color[1], color[2]);
            if (additionModel != null) {
                IVertexBuilder ivertexbuilder = bufferIn.getBuffer(RenderType.getEntityTranslucent(texture));
                additionModel.render(matrixStackIn, ivertexbuilder, packedLightIn, LivingRenderer.getOverlay(customBeeEntity, 0.0F), color[0], color[1], color[2], 1.0F);
            }
        }
    }
}
