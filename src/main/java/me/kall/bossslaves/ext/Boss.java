package me.kall.bossslaves.ext;

import it.unimi.dsi.fastutil.ints.IntSet;

public interface Boss {
    IntSet boss$slaves();
    boolean bossSlaves$isBoss();
}
