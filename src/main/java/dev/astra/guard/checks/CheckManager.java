package dev.astra.guard.checks;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import dev.astra.guard.checks.impl.*;

import java.util.ArrayList;
import java.util.List;

public final class CheckManager {
    private final List<Check> checks = new ArrayList<>();

    public CheckManager() {
        checks.add(new BundleA());
        checks.add(new ItemA());
        checks.add(new ItemB());
        checks.add(new NettyA());
        checks.add(new NettyB());
        checks.add(new NettyC());
        checks.add(new WindowA());
        checks.add(new WindowB());
        checks.add(new WindowC());
        checks.add(new FloodA());
    }

    public void handle(PacketReceiveEvent e) {
        for (Check check : checks) check.handle(e);
    }
}
