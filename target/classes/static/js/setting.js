$(function(){
    $("#uploadForm").submit(upload);
});

function upload() {
    $.ajax({
        //这里http https都可
        url: "https://upload.qiniup.com",//上传到华东区
        method: "post",
        processData: false,//表单内容不转为 字符串
        contentType: false,//不设置上传文件格式,因为文件上传是二进制的,浏览器会自动解析
        // data: $("#uploadForm").serialize(), 这个参数会报400 badrequest
        //因为new FormData的参数需要一个HTMLElement类型的数据，而jQuery得到的是个HTMLElement的集合，
        // 哪怕只有一个元素。所以需要用[]取其第一个元素。
        data: new FormData($("#uploadForm")[0]),//这里需要的是js对象 所以取集合第一个元素
        success: function(data) {
            if(data && data.code == 0) {
                // 更新头像访问路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function(data) {
                        data = $.parseJSON(data);
                        if(data.code == 0) {
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                    }
                );
            } else {
                alert("上传图片到七牛云服务器失败!");
            }
        }
    });
    return false;//如果前面的逻辑 都失败 自然就不能上传 返回false
}