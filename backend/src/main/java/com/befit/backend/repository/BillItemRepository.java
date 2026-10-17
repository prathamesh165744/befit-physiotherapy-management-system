package com.befit.backend.repository;

import com.befit.backend.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BillItemRepository extends JpaRepository<BillItem, Long> {
    List<BillItem> findByBillIdOrderByPositionAsc(Long billId);

    @Modifying(flushAutomatically = true)
    @Query("delete from BillItem i where i.bill.id = :billId")
    void deleteByBillId(@Param("billId") Long billId);
}
