package com.shiftshop.service.model.entities;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface SaleDao extends PagingAndSortingRepository<Sale, Long> {

    Optional<Sale> findByBarcode(String barcode);

}