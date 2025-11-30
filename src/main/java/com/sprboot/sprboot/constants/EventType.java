package com.sprboot.sprboot.constants;

public enum EventType {
    UNIT_REGISTERED, // unit registered by registrar
    UNIT_ASSIGNED_TO_SHIPMENT, // unit added into a shipment
    UNIT_REMOVED_FROM_SHIPMENT, // unit removed from shipment when shipment is recalled
    UNIT_SENT, // unit dispatched by the sender
    UNIT_RECEIVED, // unit received by the receiver
    UNIT_SCANNED, // unit scanned by a user
    SHIPMENT_CREATED, // shipment created by the sender
    SHIPMENT_SENT, // shipment sent by the sender
    SHIPMENT_RECEIVED, // shipment received by the receiver
    SHIPMENT_RECALLED, // shipment recalled by the sender
    SHIPMENT_SCANNED // shipment scanned by a user
}
