package com.simibubi.create.infrastructure.debugInfo.element;

import com.google.common.collect.ImmutableList;

import com.simibubi.create.infrastructure.debugInfo.InfoProvider;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record DebugInfoSection(String name, ImmutableList<InfoElement> elements) implements InfoElement {
	@Override
	public void write(Player player, FriendlyByteBuf buffer) {
		buffer.writeBoolean(true);
		buffer.writeUtf(name);
		buffer.writeCollection(elements, (buf, element) -> element.write(player, buf));
	}

	public Builder builder() {
		return builder(name).putAll(elements);
	}

	@Override
	public void print(int depth, @Nullable Player player, Consumer<String> lineConsumer) {
		String indent = Stream.generate(() -> "\t").limit(depth).collect(Collectors.joining(""));
		lineConsumer.accept(indent + "<details>");
		lineConsumer.accept(indent + "<summary>" + name + "</summary>");
		elements.forEach(element -> element.print(depth + 1, player, lineConsumer));
		lineConsumer.accept(indent + "</details>");

	}

	public static DebugInfoSection read(FriendlyByteBuf buffer) {
		String name = buffer.readUtf();
		ArrayList<InfoElement> elements = buffer.readCollection(ArrayList::new, InfoElement::read);
		return new DebugInfoSection(name, ImmutableList.copyOf(elements));
	}

	public static Builder builder(String name) {
		return new Builder(null, name);
	}

	public static DebugInfoSection of(String name, Collection<DebugInfoSection> children) {
		return builder(name).putAll(children).build();
	}

	public static class Builder {
		private final Builder parent;
		private final String name;
		private final ImmutableList.Builder<InfoElement> elements;

		public Builder(Builder parent, String name) {
			this.parent = parent;
			this.name = name;
			this.elements = ImmutableList.builder();
		}

		public Builder put(InfoElement element) {
			this.elements.add(element);
			return this;
		}

		public Builder put(String key, InfoProvider provider) {
			return put(new InfoEntry(key, provider));
		}

		public Builder put(String key, Supplier<String> value) {
			return put(key, player -> value.get());
		}

		public Builder put(String key, String value) {
			return put(key, player -> value);
		}

		public Builder putAll(Collection<? extends InfoElement> elements) {
			elements.forEach(this::put);
			return this;
		}

		public Builder section(String name) {
            return new Builder(this, name);
		}

		public Builder finishSection() {
			if (parent == null) {
				throw new IllegalStateException("Cannot finish the root section");
			}
			parent.elements.add(this.build());
			return parent;
		}

		public DebugInfoSection build() {
			return new DebugInfoSection(name, elements.build());
		}

		public void buildTo(Consumer<DebugInfoSection> consumer) {
			consumer.accept(this.build());
		}
	}
}
