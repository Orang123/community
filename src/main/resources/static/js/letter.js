$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});

function send_letter() {
	$("#sendModal").modal("hide");

	var toName = $("#recipient-name").val();
	var content = $("#message-text").val();
	$.post(
		CONTEXT_PATH + "/letter/send",
		{"toName":toName,"content":content},
		function(data) {
			data = $.parseJSON(data);
			////这里 发送私信框 点击叉叉x 就会报服务端异常,是因为global.js里默认 会把提示框的内容替换为ajax异步返回的 "服务器异常!"
			$("#hintBody").text(data.msg);
			$("#message-text").val("");
			$("#hintModal").modal("show");
			setTimeout(function(){
				$("#hintModal").modal("hide");
				if(data.code == 0)
					location.reload();
			}, 2000);
		}
	);
}

function delete_msg() {
	var btn = this;
	var id = $(btn).prev().val();
	$.post(
		CONTEXT_PATH + "/letter/delete",
		{"id":id},
		function(data) {
			data = $.parseJSON(data);
			if(data.code == 0)
				$(btn).parents(".media").remove();
				//因为boostrap框架已经将当前私信 对应的<li class="media"> 样式移除所以 无需再请求刷新当前页面了
				alert(data.msg);
		}
	);
}