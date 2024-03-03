package com.project.tableforyou.controller;import com.project.tableforyou.config.auth.PrincipalDetails;import com.project.tableforyou.domain.dto.RestaurantDto;import com.project.tableforyou.service.RestaurantService;import jakarta.servlet.ServletException;import jakarta.servlet.http.HttpServletRequest;import jakarta.servlet.http.HttpServletResponse;import lombok.Getter;import lombok.RequiredArgsConstructor;import lombok.extern.slf4j.Slf4j;import org.springframework.data.domain.Page;import org.springframework.data.domain.Pageable;import org.springframework.data.domain.Sort;import org.springframework.data.web.PageableDefault;import org.springframework.http.HttpStatus;import org.springframework.http.ResponseEntity;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.web.bind.annotation.*;import java.io.IOException;@RestController@RequestMapping("/restaurant")@RequiredArgsConstructor@Slf4jpublic class RestaurantController {    private final RestaurantService restaurantService;    /* 가게 생성 */     // 추후에 관리자가 생성 권한을 가지니 save로 날리는 게 아닌 저장시키고 관리자 페이지에서 보고 생성해야함.    @PostMapping("/create")    public ResponseEntity<String> create(@RequestBody RestaurantDto.Request dto,                                         @AuthenticationPrincipal PrincipalDetails principalDetails) {        try {            restaurantService.save(principalDetails.getUsername(), dto);            return ResponseEntity.ok("가게 생성 완료.");        } catch (Exception e) {            log.error("Error occurred while creating restaurant: {}", e.getMessage());            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가게 생성 실패.");        }    }    /* 가게 불러오기 */    @GetMapping("/{restaurant_id}")    public RestaurantDto.Response read(@PathVariable(name = "restaurant_id") Long restaurant_id) {        return restaurantService.findById(restaurant_id);    }    /* 전체 가게 불러오기. 페이징 처리 + 검색 기능 */    @GetMapping    public Page<RestaurantDto.Response> readAll(@PageableDefault(size = 20, sort = "rating", direction = Sort.Direction.DESC) Pageable pageable,                                                @RequestParam(required = false) String searchKeyword) {        if(searchKeyword == null)            return restaurantService.RestaurantPageList(pageable);        else            return restaurantService.RestaurantPageSearchList(searchKeyword, searchKeyword, pageable);    }    /* 가게 업데이트 */    @PutMapping("/{restaurant_id}")    public ResponseEntity<String> update(@PathVariable(name = "restaurant_id") Long restaurant_id, @RequestBody RestaurantDto.UpdateRequest dto,                                         @AuthenticationPrincipal PrincipalDetails principalDetails) {        try {            restaurantService.update(restaurant_id, principalDetails.getUsername(), dto);            return ResponseEntity.ok("가게 수정 완료.");        } catch (Exception e) {            log.error("Error occurred while updating restaurant: {}", e.getMessage());            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가게 수정 실패.");        }    }    /* 가게 삭제 */    @DeleteMapping("/{restaurant_id}")    public ResponseEntity<String> delete(@PathVariable(name = "restaurant_id") Long restaurant_id,                                         @AuthenticationPrincipal PrincipalDetails principalDetails) {        try {            restaurantService.delete(restaurant_id, principalDetails.getUsername());            return ResponseEntity.ok("가게 삭제 완료.");        } catch (Exception e) {            log.error("Error occurred while deleting restaurant: {}", e.getMessage());            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가게 삭제 실패.");        }    }    /* 가게 좋아요 증가 */    @PatchMapping("/{restaurant_id}/update-like")    public ResponseEntity<String> increaseLike(@PathVariable(name = "restaurant_id") Long restaurant_id, @RequestParam("like") boolean like) {        try {            int value = like ? 1 : -1;            restaurantService.updateLikeCount(restaurant_id, value);            String action = like ? "증가" : "감소";            return ResponseEntity.ok("가게 좋아요 " + action + "완료.");        } catch (Exception e) {            log.error("Error occurred while updating restaurant like count: {}", e.getMessage());            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가게 좋아요 업데이트 실패.");        }    }    /* 가게 평점 업데이트*/    @PatchMapping("/{restaurant_id}/update-rating")    public ResponseEntity<String> updateRating(@PathVariable(name = "restaurant_id") Long restaurant_id, @RequestBody RatingDto rating) {        try {            restaurantService.updateRating(restaurant_id, rating.getRating());            return ResponseEntity.ok("가게 평점 업데이트 완료.");        } catch (Exception e) {            log.error("Error occurred while updating restaurant rating: {}", e.getMessage());            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가게 평점 업데이트 실패.");        }    }    /* 가게 평점을 객체로 가져오기 위해. */    @Getter    private static class RatingDto {        private double rating;    }     /* 좌석 업데이트 (forward처리를 하는 주소.) */    @PatchMapping("/{restaurant_id}/update-usedSeats")    public void updateFullUsedSeats(@PathVariable(name = "restaurant_id") Long restaurant_id, @RequestParam("increase") boolean increase,                                                      HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {        RestaurantDto.Response restaurant = restaurantService.findById(restaurant_id);        int value = increase ? 1 : -1;        String forwardUrl;        if(value == -1 && restaurant.getReservationSize() != 0) {   // 좌석이 다 차서 예약자에서 인원을 가져올 때. (인원이 줄면)            forwardUrl = "/restaurant/" + restaurant_id + "/reservation/decreaseBooking";        }        else {                                                          // 좌석이 덜 찼을 때            forwardUrl = "/restaurant/" + restaurant_id + "/update-usedSeats/" + value;        }        request.getRequestDispatcher(forwardUrl).forward(request, response);        // forward는 redirect와는 달리 클라이언트에 보여지는게 아님.    }     /* 좌석 업데이트 */    @PatchMapping("/{restaurant_id}/update-usedSeats/{value}")    public ResponseEntity<String> updateUsedSeats(@PathVariable(name = "restaurant_id") Long restaurant_id, @PathVariable int value) {        RestaurantDto.Response restaurant = restaurantService.findById(restaurant_id);        if(value == 1 && restaurant.getUsedSeats() < restaurant.getTotalSeats()) {           restaurantService.updateUsedSeats(restaurant_id, value);            return ResponseEntity.ok("가게 좌석 증가 완료.");        } else if (value == -1) {            restaurantService.updateUsedSeats(restaurant_id, value);            return ResponseEntity.ok("가게 좌석 감소 완료.");        } else {            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가게 좌석 업데이트 실패.");        }    }}