$(function(){
    $("#topBtn").click(setTop);
    $("#wonderfulBtn").click(setWonderful);
    $("#deleteBtn").click(setDelete);
});


function like(btn, entityType, entityId, entityUserId, postId) {
    $.post(
        CONTEXT_PATH + "/like",
        {"entityType":entityType,"entityId":entityId,"entityUserId":entityUserId,"postId":postId},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus==1?'已赞':"赞");
            } else {
                alert(data.msg);
            }
        }
    );
}

// 置顶
function setTop() {
    $.post(
        CONTEXT_PATH + "/discuss/top",
        {"postId":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            alert(data.msg);
            if(data.code == 0) {
                $("#topBtn").attr("disabled", "disabled");
            }
        }
    );
}

// 加精
function setWonderful() {
    $.post(
        CONTEXT_PATH + "/discuss/wonderful",
        {"postId":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            alert(data.msg);
            if(data.code == 0) {
                $("#wonderfulBtn").attr("disabled", "disabled");
            }
        }
    );
}

// 删除
function setDelete() {
    $.post(
        CONTEXT_PATH + "/discuss/delete",
        {"postId":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            alert(data.msg);
            if(data.code == 0) {
                location.href = CONTEXT_PATH + "/index";
            }
        }
    );
}