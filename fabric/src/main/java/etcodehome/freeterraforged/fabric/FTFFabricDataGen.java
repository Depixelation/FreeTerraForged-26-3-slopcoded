package etcodehome.freeterraforged.fabric;

import etcodehome.freeterraforged.client.data.FTFLanguageProvider;
import etcodehome.freeterraforged.client.data.FTFTranslationKeys;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator.Pack;
import net.minecraft.data.DataProvider;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;

public class FTFFabricDataGen implements DataGeneratorEntrypoint {

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		Pack pack = fabricDataGenerator.createPack();

		pack.addProvider((DataProvider.Factory<FTFLanguageProvider.EnglishUS>) FTFLanguageProvider.EnglishUS::new);
		pack.addProvider((DataProvider.Factory<PackMetadataGenerator>) output -> PackMetadataGenerator.forFeaturePack(output, Component.translatable(FTFTranslationKeys.METADATA_DESCRIPTION)));
	}
}
