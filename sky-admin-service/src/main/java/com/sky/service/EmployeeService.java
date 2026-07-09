package com.sky.service;

import com.sky.dto.admin.EmployeeDTO;
import com.sky.dto.admin.EmployeeLoginDTO;
import com.sky.dto.admin.EmployeePageQueryDTO;
import com.sky.dto.admin.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO
     * @return
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * 员工分页查询
     * @param employeePageQueryDTO
     */
    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 设置员工状态
     * @param status
     * @param id
     */
    void updateStatus(Integer status, Long id);

    /**
     * 查询员工信息
     * @param id
     */
    Employee getById(Long id);

    /**
     * 更新员工信息
     * @param employeeDTO
     */
    void updateEmployee(EmployeeDTO employeeDTO);

    /**
     * 修改密码
     * @param passwordEditDTO
     */
    void editPasswordById(PasswordEditDTO passwordEditDTO);
}
