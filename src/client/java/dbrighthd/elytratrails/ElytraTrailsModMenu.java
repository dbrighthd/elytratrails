package dbrighthd.elytratrails;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
//import dbrighthd.elytratrails.config.ConfigScreenBuilder;
import dbrighthd.elytratrails.config.ConfigScreenBuilder;
import dbrighthd.elytratrails.config.FallbackConfigMessageScreen;

import static dbrighthd.elytratrails.ElytraTrailsClient.getConfig;
import static dbrighthd.elytratrails.compat.ModStatuses.CLOTH_LOADED;

public class ElytraTrailsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if(CLOTH_LOADED) {
            return parent -> ConfigScreenBuilder.buildConfigScreen(parent, getConfig());
        }
        return FallbackConfigMessageScreen::new;
    }

}