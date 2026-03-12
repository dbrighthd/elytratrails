package dbrighthd.elytratrails;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dbrighthd.elytratrails.config.ConfigScreenBuilder;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;

public class ElytraTrailsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> ConfigScreenBuilder.buildConfigScreen(parent, getConfig());
    }

}