package dbrighthd.elytratrails.network;

public class CompiledPlayerConfig {
    public PlayerConfig playerConfigInitial;
    public PlayerConfigExtended playerConfigExtended;

    public CompiledPlayerConfig(PlayerConfig playerConfig)
    {
        playerConfigInitial = playerConfig;
        playerConfigExtended = ClientPlayerConfigStore.getClientOthersConfigExtended();
    }
    public CompiledPlayerConfig(PlayerConfigExtended playerConfigExtended)
    {
        this.playerConfigInitial = ClientPlayerConfigStore.getClientOthersConfigInitial();
        this.playerConfigExtended = playerConfigExtended;
    }
    public CompiledPlayerConfig(PlayerConfig playerConfigInitial, PlayerConfigExtended playerConfigExtended)
    {
        this.playerConfigInitial = playerConfigInitial;
        this.playerConfigExtended = playerConfigExtended;
    }
    public void updateInitialConfig(PlayerConfig playerConfigInitial)
    {
        this.playerConfigInitial = playerConfigInitial;
    }
    public void updateExtendedConfig(PlayerConfigExtended playerConfigExtended)
    {
        this.playerConfigExtended = playerConfigExtended;
    }
}
