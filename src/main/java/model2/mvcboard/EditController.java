package model2.mvcboard;

import java.io.IOException;

import fileupload.FileUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import utils.JSFunction;

//수정페이지에 대한 매핑
@WebServlet("/mvcboard/edit.do")
//파일업로드 설정
@MultipartConfig(
	maxFileSize = 1024 * 1024 * 10,
	maxRequestSize = 1024 * 1024 * 100
)
public class EditController extends HttpServlet {
	
	//수정페이지로 진입
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		//로그인 확인
		HttpSession session = req.getSession();
		if (session.getAttribute("UserId")==null) {
			JSFunction.alertLocation(resp, "로그인 후 이용해주세요.", "../06Session/LoginForm.jsp");
			return;
		}
		
		//게시물 얻어오기
		//일련번호를 파라미터로 받아옴
		String idx = req.getParameter("idx");
		MVCBoardDAO dao = new MVCBoardDAO();
		//열람에서 사용했던 메서드를 통해 기존 내용 가져오기
		MVCBoardDTO dto = dao.selectView(idx);
		
		//작성자 본인 확인
		if(!dto.getId().equals(session.getAttribute("UserId").toString())) {
			JSFunction.alertBack(resp, "작성자 본인만 수정할 수 있습니다.");
			return;
		}
		
		//DTO 인스턴스를 request 영역에 저장 후 포워드
		req.setAttribute("dto", dto);
		req.getRequestDispatcher("/14MVCBoard/Edit.jsp").forward(req, resp);
		
	}
	
	// 수정처리
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		//로그인 확인
		HttpSession session = req.getSession();
		if (session.getAttribute("UserId")==null) {
			JSFunction.alertLocation(resp, "로그인 후 이용해주세요.", "../06Session/LoginForm.jsp");
			return;
		}
		
		//작성자 본인 확인
		if(!req.getParameter("id").equals(session.getAttribute("UserId").toString())) {
			JSFunction.alertBack(resp, "작성자 본인만 수정할 수 있습니다.");
			return;
		}
		
				
		//1. 파일 업로드 처리 ====================
		// 업로드 디렉터리(서버)의 물리적 경로 확인
		String saveDirectory = req.getServletContext().getRealPath("/Uploads");
		
		//파일 업로드 처리
		String originalFileName = "";
		try {
			originalFileName = FileUtil.uploadFile(req, saveDirectory);
		} catch (Exception e) {
			JSFunction.alertBack(resp, "파일 업로드 오류입니다.");
			return;
		}
		
		// 2. 파일 업로드 외 처리 ======================
		// 수정 내용을 매개변수에서 얻어옴
		// 히든 상자에 추가된 값 읽어오기
		// 일련번호
		String idx = req.getParameter("idx"); 
		// 기존 등록되 파일명
		String prevOfile = req.getParameter("prevOfile");
		String prevSfile = req.getParameter("prevSfile");
		
		//사용자가 입력한 값
		String title = req.getParameter("title");
		String content = req.getParameter("content");
		
		//DTO 인스턴스 생성 및 수정할 값 저장
		MVCBoardDTO dto = new MVCBoardDTO();
		dto.setIdx(idx);
		dto.setId(session.getAttribute("UserId").toString());
		dto.setTitle(title);
		dto.setContent(content);
		
		//원본 파일명과 저장된 파일 이름 설정
		if (originalFileName != "") {
			//첨부파일이 있는 경우
			//서버에 저장된 파일명을 날짜형식으로 변경
			String savedFileName = FileUtil.renameFile(saveDirectory, originalFileName);
			
			//새롭게 첨부한 파일명을 DTO에 저장
			dto.setOfile(originalFileName);	//원래 파일 이름
			dto.setSfile(savedFileName);	//서버에 저장된 파일이름
			
			//기존 파일 삭제
			FileUtil.deleteFile(req, "/Uploads", prevSfile);
		}
		else {
			//첨부 파일이 없는 경우 => 기존 이름 유지(hidden상자에 설정한 값)
			dto.setOfile(prevOfile);
			dto.setSfile(prevSfile);
		}
		
		//DB에 수정내용 반영, update 쿼리문 실행
		MVCBoardDAO dao = new MVCBoardDAO();
		int result = dao.updatePost(dto);
		dao.close();
		
		// 성공 or 실패?
		if (result == 1) { // 수정성공
			resp.sendRedirect("../mvcboard/view.do?idx=" + idx);
		}
		else {	// 수정 실패 => 경고창을 띄움.
			JSFunction.alertLocation(resp, "수정에 실패했습니다.", 
					"../mvcboard/view.do?idx=" + idx);
		}
		
	}
}
