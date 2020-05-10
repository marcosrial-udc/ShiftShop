package com.shiftshop.service.rest.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.shiftshop.service.model.common.exceptions.DuplicateInstancePropertyException;
import com.shiftshop.service.model.common.exceptions.InstanceNotFoundException;
import com.shiftshop.service.model.entities.Category;
import com.shiftshop.service.model.entities.Product;
import com.shiftshop.service.model.entities.User;
import com.shiftshop.service.model.services.*;
import com.shiftshop.service.rest.dtos.sale.InsertSaleItemParamsDto;
import com.shiftshop.service.rest.dtos.sale.InsertSaleParamsDto;
import com.shiftshop.service.rest.dtos.user.AuthenticatedUserDto;
import com.shiftshop.service.rest.dtos.user.LoginParamsDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SaleControllerTest {

    private final Long NON_EXISTENT_ID = new Long(-1);
    private final String USERNAME = "user";
    private final static String PASSWORD = "password";
    private final String NAME = "User";
    private final String SURNAMES = "Test Tester";
    private final String CATEGORY_NAME = "category";
    private final String PRODUCT_NAME = "product";
    private final BigDecimal PROV_PRICE = new BigDecimal(5.25);
    private final BigDecimal SALE_PRICE = new BigDecimal(21.95);
    private final String BARCODE = "ABCD1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserController userController;


    private AuthenticatedUserDto createAuthenticatedUser(String userName, Set<User.RoleType> roles)
            throws IncorrectLoginException, UserNotActiveException,
            DuplicateInstancePropertyException, NoUserRolesException {

        User user = new User(userName, PASSWORD, NAME, SURNAMES, roles);

        userService.registerUser(user);

        LoginParamsDto loginParams = new LoginParamsDto();
        loginParams.setUserName(user.getUserName());
        loginParams.setPassword(PASSWORD);

        return userController.login(loginParams);
    }

    private AuthenticatedUserDto createAuthenticatedSalesmanUser(String userName)
            throws IncorrectLoginException, UserNotActiveException,
            DuplicateInstancePropertyException, NoUserRolesException {

        Set<User.RoleType> roles = new HashSet<>();
        roles.add(User.RoleType.SALESMAN);

        return createAuthenticatedUser(userName, roles);
    }

    private Category createCategory(String name) throws DuplicateInstancePropertyException {
        return catalogService.addCategory(name);
    }

    private Product createProduct(String name, Long categoryId)
            throws DuplicateInstancePropertyException, InstanceNotFoundException {

        return catalogService.addProduct(name, PROV_PRICE, SALE_PRICE, categoryId);

    }

    @Test
    public void testRegisterSale_NoContent() throws Exception {

        AuthenticatedUserDto user = createAuthenticatedSalesmanUser(USERNAME);
        Category category = createCategory(CATEGORY_NAME);
        Product product = createProduct(PRODUCT_NAME, category.getId());

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Sale items params
        InsertSaleItemParamsDto paramItems = new InsertSaleItemParamsDto();
        paramItems.setProductId(product.getId());
        paramItems.setSalePrice(SALE_PRICE);
        paramItems.setQuantity(2);

        // Sale params without discount
        InsertSaleParamsDto params = new InsertSaleParamsDto();
        params.setBarcode(BARCODE);
        params.setDate(LocalDateTime.now().withNano(0));
        params.setItems(new HashSet<>(Arrays.asList(paramItems)));
        params.setSellerId(user.getUserLoggedDto().getId());

        mockMvc.perform(put("/sales")
                .header("Authorization", "Bearer " + user.getServiceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(params)))
                .andExpect(status().isNoContent());

        // With cash
        params.setCash(new BigDecimal(500));
        mockMvc.perform(put("/sales")
                .header("Authorization", "Bearer " + user.getServiceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(params)))
                .andExpect(status().isNoContent());

    }

    @Test
    public void testRegisterSale_BadRequest() throws Exception {

        AuthenticatedUserDto user = createAuthenticatedSalesmanUser(USERNAME);
        Category category = createCategory(CATEGORY_NAME);
        Product product = createProduct(PRODUCT_NAME, category.getId());

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Sale items params
        InsertSaleItemParamsDto paramItems = new InsertSaleItemParamsDto();
        paramItems.setProductId(product.getId());
        paramItems.setSalePrice(SALE_PRICE);
        paramItems.setQuantity(2);

        // Sale params
        InsertSaleParamsDto params = new InsertSaleParamsDto();
        params.setBarcode(BARCODE);
        params.setDate(LocalDateTime.now().withNano(0));
        params.setItems(new HashSet<>(Arrays.asList(paramItems)));
        params.setSellerId(user.getUserLoggedDto().getId());


        // Test no barcode
        params.setBarcode(null);

        mockMvc.perform(put("/sales")
                .header("Authorization", "Bearer " + user.getServiceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(params)))
                .andExpect(status().isBadRequest());

        params.setBarcode(BARCODE);

        // Test no date
        params.setDate(null);

        mockMvc.perform(put("/sales")
                .header("Authorization", "Bearer " + user.getServiceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(params)))
                .andExpect(status().isBadRequest());

        params.setDate(LocalDateTime.now());

        // Test negative discount
        params.setDiscount(new BigDecimal(-1));

        mockMvc.perform(put("/sales")
                .header("Authorization", "Bearer " + user.getServiceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(params)))
                .andExpect(status().isBadRequest());

        params.setDiscount(null);

        // Test no items
        params.setItems(new HashSet<>());

        mockMvc.perform(put("/sales")
                .header("Authorization", "Bearer " + user.getServiceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(params)))
                .andExpect(status().isBadRequest());

        params.setItems(new HashSet<>(Arrays.asList(paramItems)));

        // Test invalid cash amount
        params.setCash(new BigDecimal(0));

        mockMvc.perform(put("/sales")
                .header("Authorization", "Bearer " + user.getServiceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(params)))
                .andExpect(status().isBadRequest());

        params.setCash(null);

        // Test items with bad quantity and negative price
        paramItems.setQuantity(-1);

        mockMvc.perform(put("/sales")
                .header("Authorization", "Bearer " + user.getServiceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(params)))
                .andExpect(status().isBadRequest());

        paramItems.setQuantity(2);

        // Test items with bad quantity and negative price
        paramItems.setSalePrice(new BigDecimal(-1));

        mockMvc.perform(put("/sales")
                .header("Authorization", "Bearer " + user.getServiceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(params)))
                .andExpect(status().isBadRequest());

    }

    @Test
    public void testRegisterSale_NotFound() throws Exception {

        AuthenticatedUserDto user = createAuthenticatedSalesmanUser(USERNAME);
        Category category = createCategory(CATEGORY_NAME);
        Product product = createProduct(PRODUCT_NAME, category.getId());

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Sale items params
        InsertSaleItemParamsDto paramItems = new InsertSaleItemParamsDto();
        paramItems.setProductId(product.getId());
        paramItems.setSalePrice(SALE_PRICE);
        paramItems.setQuantity(2);

        // Sale params without discount
        InsertSaleParamsDto params = new InsertSaleParamsDto();
        params.setBarcode(BARCODE);
        params.setDate(LocalDateTime.now().withNano(0));
        params.setItems(new HashSet<>(Arrays.asList(paramItems)));

        // Seller no existent
        params.setSellerId(NON_EXISTENT_ID);
        mockMvc.perform(put("/sales")
                .header("Authorization", "Bearer " + user.getServiceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(params)))
                .andExpect(status().isNotFound());

        params.setSellerId(user.getUserLoggedDto().getId());

        // Product no existent
        paramItems.setProductId(NON_EXISTENT_ID);
        mockMvc.perform(put("/sales")
                .header("Authorization", "Bearer " + user.getServiceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(params)))
                .andExpect(status().isNotFound());

    }

}