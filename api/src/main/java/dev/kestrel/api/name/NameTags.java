package dev.kestrel.api.name;

import dev.kestrel.api.Subscription;

public interface NameTags {
    Subscription register(NameDecorator decorator);
}
