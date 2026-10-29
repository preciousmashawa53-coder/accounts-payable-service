package zw.co.preciousmashawa.ap.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import zw.co.preciousmashawa.ap.service.AccountsPayableService;
import zw.co.preciousmashawa.ap.web.Dtos.CreateSupplierRequest;
import zw.co.preciousmashawa.ap.web.Dtos.SupplierResponse;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final AccountsPayableService service;

    public SupplierController(AccountsPayableService service) {
        this.service = service;
    }

    @GetMapping
    public List<SupplierResponse> list() {
        return service.listSuppliers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLERK')")
    public SupplierResponse create(@Valid @RequestBody CreateSupplierRequest request, Principal user) {
        return service.createSupplier(request, user.getName());
    }
}
