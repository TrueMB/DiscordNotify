package me.truemb.discordnotify.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;

import org.simpleyaml.configuration.file.YamlConfiguration;
import org.simpleyaml.exceptions.InvalidConfigurationException;
import org.simpleyaml.utils.Validate;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class UTF8YamlConfiguration extends YamlConfiguration {

	public UTF8YamlConfiguration(File file) {
		try {
			load(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void save(File file) throws IOException {
		Validate.notNull(file, "File cannot be null");
		Files.createParentDirs(file);
		String data = this.saveToString();
		if(data == null) return;
		Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8);

		try {
			writer.write(data);
		} finally {
			writer.close();
		}
	}

	@Override
	public String saveToString() {
		try {

			Field fy = Reflections.getField(getClass(), "yamlOptions");
			Field fr = Reflections.getField(getClass(), "yamlRepresenter");
			Field fYaml = Reflections.getField(getClass(), "yaml");

			Reflections.setAccessible(true, fy, fr, fYaml);

			DumperOptions yamlOptions = (DumperOptions) fy.get(this);
			Representer yamlRepresenter = (Representer) fr.get(this);
			Yaml yaml = (Yaml) fYaml.get(this);
			DumperOptions.FlowStyle fs = DumperOptions.FlowStyle.BLOCK;

			yamlOptions.setIndent(this.options().indent());
			yamlOptions.setDefaultFlowStyle(fs);
			yamlOptions.setAllowUnicode(true);
			yamlRepresenter.setDefaultFlowStyle(fs);

			String dump = yaml.dump(this.getValues(false));

			if (dump.equals("{}\n"))
				dump = "";

			return dump;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void load(File file) throws FileNotFoundException, IOException, InvalidConfigurationException {
		Validate.notNull(file, "File cannot be null");
		this.load(new InputStreamReader(new FileInputStream(file), Charsets.UTF_8));
	}
}