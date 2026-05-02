package dbrighthd.elytratrails.config;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.core.particles.ParticleOptions;

import java.io.IOException;

import static dbrighthd.elytratrails.handler.ParticleHandler.decodeParticle;
import static dbrighthd.elytratrails.handler.ParticleHandler.encodeParticle;


public class ParticleAdapter extends TypeAdapter<ParticleOptions> {
    @Override
    public void write(JsonWriter jsonWriter, ParticleOptions particleOptions) throws IOException {
        String out = encodeParticle(particleOptions);
        jsonWriter.value(out);
    }

    @Override
    public ParticleOptions read(JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }
        String particleString = jsonReader.nextString();
        return decodeParticle(particleString);
    }
}
