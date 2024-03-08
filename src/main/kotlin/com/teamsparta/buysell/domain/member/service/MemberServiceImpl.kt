package com.teamsparta.buysell.domain.member.service

import com.teamsparta.buysell.domain.exception.ModelNotFoundException
import com.teamsparta.buysell.domain.member.dto.request.LoginRequest
import com.teamsparta.buysell.domain.member.dto.request.MemberProfileUpdateRequest
import com.teamsparta.buysell.domain.member.dto.request.SignUpRequest
import com.teamsparta.buysell.domain.member.dto.response.MemberResponse
import com.teamsparta.buysell.domain.member.model.Member
import com.teamsparta.buysell.domain.member.model.Platform
import com.teamsparta.buysell.domain.member.model.Role
import com.teamsparta.buysell.domain.member.repository.MemberRepository
import com.teamsparta.buysell.domain.post.dto.response.PostResponse
import com.teamsparta.buysell.domain.post.model.toResponse
import com.teamsparta.buysell.domain.post.repository.LikeRepository
import com.teamsparta.buysell.domain.post.repository.PostRepository
import com.teamsparta.buysell.infra.security.UserPrincipal
import com.teamsparta.buysell.infra.security.jwt.JwtPlugin
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberServiceImpl(
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val likeRepository: LikeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtPlugin: JwtPlugin,
): MemberService {
    override fun signUp(request: SignUpRequest): MemberResponse {
        memberRepository.findByEmail(request.email)?.let {
            throw DataIntegrityViolationException("이미 존재하는 이메일입니다.")
        }
        val member = Member(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            nickname = request.nickname,
            role = Role.MEMBER,
            gender = request.gender,
            birthday = request.birthday,
            platform = Platform.LOCAL,
        )
        memberRepository.save(member)
        return member.toResponse()
    }

    override fun login(request: LoginRequest): String {
        val member = memberRepository.findByEmail(request.email)
            ?: throw BadCredentialsException("이메일이 존재하지 않거나 틀렸습니다.")
        if(!passwordEncoder.matches(request.password,member.password)){
            throw BadCredentialsException("비밀번호가 존재하지 않거나 틀렸습니다.")
        }
        val token = jwtPlugin.generateAccessToken(member.id.toString(), member.email, member.role.toString(), member.platform.toString())
        return token
    }


    @Transactional
    override fun getMember(userPrincipal: UserPrincipal): MemberResponse? {
        val member = memberRepository.findByIdOrNull(userPrincipal.id)
            ?: throw ModelNotFoundException("Member", userPrincipal.id)
        return member.toResponse()
    } // 멤버 아이디 기준 정보 조회

    @Transactional
    override fun updateMember(userPrincipal: UserPrincipal, request: MemberProfileUpdateRequest): MemberResponse {
        val member = memberRepository.findByIdOrNull(userPrincipal.id)
            ?: throw ModelNotFoundException("Member", userPrincipal.id)
        member.nickname = request.nickname
        member.birthday = request.birthday
       return memberRepository.save(member).toResponse()
    } // 멤버 아이디 기준 정보 수정

    override fun getAllPostByUserPrincipal(userPrincipal: UserPrincipal): List<PostResponse>? {
        val member = memberRepository.findByIdOrNull(userPrincipal.id)
            ?: throw ModelNotFoundException("member", userPrincipal.id)
        val post = postRepository.findAllByMember(member)
        return post.map { it.toResponse() }
    }//내가 쓴 글 전체 조회

    override fun getAllPostByLike(userPrincipal: UserPrincipal): List<PostResponse>? {
        val member = memberRepository.findByIdOrNull(userPrincipal.id)
            ?: throw ModelNotFoundException("member", userPrincipal.id)
        val like = likeRepository.findByMember(member)
        val post = like.map { it.post }
        return post.map { it.toResponse() }
    }//내가 찜 한 글 전체 조회

    override fun pretendDelete(userPrincipal: UserPrincipal) {
        val member = memberInformation(userPrincipal)

        memberRepository.delete(member)
    } //회원 탈퇴 요청

    private fun memberInformation(userPrincipal: UserPrincipal) = memberRepository.findByIdOrNull(userPrincipal.id)
        ?:throw ModelNotFoundException("member",userPrincipal.id)

}
