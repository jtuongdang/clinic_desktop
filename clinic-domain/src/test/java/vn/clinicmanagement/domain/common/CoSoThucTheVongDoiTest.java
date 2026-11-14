package vn.clinicmanagement.domain.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoSoThucTheVongDoiTest {

    @Test
    void lifecycleActorsMustBePositiveAndLifecycleDoesNotAdvanceVersion() {
        TestEntity entity = new TestEntity();
        entity.setVersion(7L);

        entity.markAsUpdated(12);
        assertEquals(12, entity.getUpdatedBy());
        assertEquals(7L, entity.getVersion());

        entity.softDelete(13);
        assertTrue(entity.isDeleted());
        assertEquals(13, entity.getDeletedBy());
        assertEquals(7L, entity.getVersion());

        entity.restore();
        assertFalse(entity.isDeleted());
        assertNull(entity.getDeletedBy());
        assertEquals(7L, entity.getVersion());
    }

    @Test
    void rejectsNonPositiveActorsInLifecycleMethodsAndNullableMetadataSetters() {
        TestEntity entity = new TestEntity();

        assertThrows(IllegalArgumentException.class, () -> entity.markAsUpdated(0));
        assertThrows(IllegalArgumentException.class, () -> entity.markAsUpdated(-1));
        assertThrows(IllegalArgumentException.class, () -> entity.softDelete(0));
        assertThrows(IllegalArgumentException.class, () -> entity.softDelete(-1));
        assertThrows(IllegalArgumentException.class, () -> entity.setUpdatedBy(0));
        assertThrows(IllegalArgumentException.class, () -> entity.setUpdatedBy(-1));
        assertThrows(IllegalArgumentException.class, () -> entity.setDeletedBy(0));
        assertThrows(IllegalArgumentException.class, () -> entity.setDeletedBy(-1));

        entity.setUpdatedBy(null);
        entity.setDeletedBy(null);
        assertNull(entity.getUpdatedBy());
        assertNull(entity.getDeletedBy());
    }

    @Test
    void equalityUsesConcreteClassAndPreservesNullIdBehavior() {
        TestEntity first = new TestEntity();
        TestEntity second = new TestEntity();
        OtherTestEntity otherClass = new OtherTestEntity();

        assertEquals(first, first);
        assertNotEquals(first, second);
        assertNotEquals(first, otherClass);
        int transientHashCode = first.hashCode();

        first.setId(42);
        second.setId(42);
        otherClass.setId(42);
        assertEquals(first, second);
        assertNotEquals(first, otherClass);
        assertEquals(transientHashCode, first.hashCode());

        second.setId(43);
        assertNotEquals(first, second);
        assertEquals(transientHashCode, second.hashCode());
    }

    private static class TestEntity extends CoSoThucThe<Integer> {
    }

    private static final class OtherTestEntity extends CoSoThucThe<Integer> {
    }
}
