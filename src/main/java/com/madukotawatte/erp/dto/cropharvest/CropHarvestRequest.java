package com.madukotawatte.erp.dto.cropharvest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CropHarvestRequest {

    /** Cocoa | Pepper | Cinnamon | Coffee | Arecanut | Cloves | Other */
    @NotBlank
    private String crop;

    @NotNull
    private LocalDate harvestDate;

    @NotNull
    @Positive
    private BigDecimal quantity;

    /** kg | g | nuts | bunches | litres | units | cubic_ft | other */
    @NotBlank
    private String unit;

    @Size(max = 100)
    private String fieldBlock;

    private String employeeId;

    @Size(max = 500)
    private String notes;
}
