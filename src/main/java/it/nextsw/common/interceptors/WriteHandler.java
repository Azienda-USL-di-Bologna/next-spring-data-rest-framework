package it.nextsw.common.interceptors;

//package it.bologna.ausl.common.controller;
//
//import java.io.Serializable;
//
//import org.hibernate.NextSdrEmptyControllerInterceptor;
//import org.hibernate.Transaction;
//import org.hibernate.type.Type;
//
//public class WriteHandler extends NextSdrEmptyControllerInterceptor {
//
//    private int updates;
//    private int creates;
//    private int loads;
//
//    @Override
//    public void onDelete(Object entities,
//            Serializable id,
//            Object[] state,
//            String[] propertyNames,
//            Type[] types) {
//        System.out.println("onDelete");
//    }
//
//    @Override
//    public boolean onFlushDirty(Object entities,
//            Serializable id,
//            Object[] currentState,
//            Object[] previousState,
//            String[] propertyNames,
//            Type[] types) {
//        System.out.println("onFlushDirty");
//        return false;
//    }
//
//    @Override
//    public boolean onLoad(Object entities,
//            Serializable id,
//            Object[] state,
//            String[] propertyNames,
//            Type[] types) {
//        System.out.println("onLoad");
//        return false;
//    }
//
//    @Override
//    public boolean onSave(Object entities,
//            Serializable id,
//            Object[] state,
//            String[] propertyNames,
//            Type[] types) {
//        System.out.println("onSave");
//        return false;
//    }
//
//    @Override
//    public void afterTransactionCompletion(Transaction tx) {
//        System.out.println("afterTransactionCompletion");
//        System.out.println("Creations: " + creates + ", Updates: " + updates + "Loads: " + loads);
//
//        updates = 0;
//        creates = 0;
//        loads = 0;
//    }
//
//}
