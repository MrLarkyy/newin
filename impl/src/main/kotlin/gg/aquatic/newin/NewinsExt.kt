package gg.aquatic.newin

import lol.farsight.newin.registrar.Newins
import org.bukkit.plugin.java.JavaPlugin

fun JavaPlugin.registerNewins(packageStr: String) {
    Newins.INSTANCE.applyToPackage(this, packageStr)
}

fun Collection<Class<*>>.registerNewins() {
    Newins.INSTANCE.applyToClasses(*this.toTypedArray())
}