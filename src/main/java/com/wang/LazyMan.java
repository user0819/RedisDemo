package com.wang;

/**
 * Created by Mr.Wang on 2019/2/17.
 */
public class LazyMan {
    private static LazyMan lazyMan = null;

    private LazyMan(String dateTime){
        System.out.print("....dateTime...Do Something.....");
    }

    /**
     * 此方法没有考虑到多线程
     * 多个线程同时进入，有可能会产生多个实例，不再是单例
     * @param dateTime
     * @return
     */
    public static LazyMan method1(String dateTime){
        if(lazyMan == null){
            lazyMan = new LazyMan(dateTime);
        }
        return lazyMan;
    }

    /**
     * 此方法避免了多线程产生多实例的可能
     * 但将整个方法锁住，影响性能
     * @param dateTime
     * @return
     */
    public static synchronized LazyMan method2(String dateTime){
        if(lazyMan == null){
            lazyMan = new LazyMan(dateTime);
        }
        return lazyMan;
    }

    /**
     * 使用双重判断
     * 既保证了单一实例，又提高了速度
     * @param dateTime
     * @return
     */
    public static LazyMan method3(String dateTime){

        if(lazyMan == null){
            synchronized (LazyMan.class) {
                if(lazyMan == null){
                    lazyMan = new LazyMan(dateTime);
                }
            }
        }
        return lazyMan;
    }
}
