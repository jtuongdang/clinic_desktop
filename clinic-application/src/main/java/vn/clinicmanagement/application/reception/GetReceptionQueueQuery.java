package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.common.Query;

import java.time.LocalDate;

public record GetReceptionQueueQuery(LocalDate date) implements Query<ReceptionQueueSnapshot> {}
