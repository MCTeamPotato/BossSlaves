package me.kall.bossslaves.ext;

public interface Slave {
    boolean boss$isSlave();
    void boss$setSlave(boolean isSlave);
    int boss$slaveOwner();
    void boss$setSlaveOwner(int boss);
}
