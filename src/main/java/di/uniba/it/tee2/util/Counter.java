/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2.util;

/**
 *
 * @author pierpaolo
 */
public class Counter {

    private static int counter = 0;

    public static synchronized void init() {
        counter = 0;
    }

    public static synchronized int increment() {
        counter++;
        return counter;
    }

    public static synchronized int get() {
        return counter;
    }

}
