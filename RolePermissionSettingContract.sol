pragma solidity 0.5.8;

// 引入Identity Manage Contract（IMC）接口，用于身份验证
interface IIdentityManageContract {
    function verifyIdentity(bytes calldata pubkEi, bytes calldata sysIDi) external view returns (bool);
}

contract RolePermissionSettingContract {
    // 定义文件权限结构体，对应算法3中的FilePermission
    struct FilePermission {
        uint256 personal;   // 个人信息权限（0：无权限，1：有权限）
        uint256 medical;    // 医疗检测报告权限
        uint256 diagnosis;  // 诊断报告权限
    }

    // EMRsRoleAccess：Fid => 角色 => 文件权限（核心权限映射）
    mapping(bytes32 => mapping(string => FilePermission)) public emrsRoleAccess;

    // IMC合约地址，用于身份验证
    IIdentityManageContract public imcContract;

    // 构造函数：初始化IMC合约地址
    constructor(address _imcAddress) public {
        imcContract = IIdentityManageContract(_imcAddress);
    }

    /**
     * @dev 设置EMR的角色权限（对应算法3的核心逻辑）
     * @param fid 电子病历唯一标识（Fid）
     * @param roles 角色列表（与permissions一一对应）
     * @param permissions 权限列表（每个角色对应的权限）
     * @param pubkEi 实体公钥（用于身份验证）
     * @param sysIDi 实体系统ID（用于身份验证）
     * @param sigi 实体签名（简化处理，实际需链下验证后传入）
     * @return 操作结果（成功/失败）
     */
    function setPermission(
        bytes32 fid,
        string[] calldata roles,
        FilePermission[] calldata permissions,
        bytes calldata pubkEi,
        bytes calldata sysIDi,
        bytes calldata sigi
    ) external returns (bool) {
        // 1. 身份验证：调用IMC合约验证实体合法性（对应算法3 lines 4-7）
        bool isIdentityValid = imcContract.verifyIdentity(pubkEi, sysIDi);
        require(isIdentityValid, "Identity verification failed");

        // 2. 验证角色与权限列表长度一致
        require(roles.length == permissions.length, "Roles and permissions length mismatch");

        // 3. 简化IPFS存在性检查：智能合约无法直接访问IPFS，此处假设Fid有效性已通过链下验证
        // （算法3 lines 8-10的IPFS检查在链下完成，仅保留结果验证）
        require(fid != bytes32(0), "Invalid Fid (IPFS existence checked off-chain)");

        // 4. 迭代权限设置，更新EMRsRoleAccess（对应算法3 lines 11-20）
        for (uint256 i = 0; i < roles.length; i++) {
            string memory role = roles[i];
            FilePermission memory perm = permissions[i];
            // 验证权限值合法性（0或1）
            require(perm.personal == 0 || perm.personal == 1, "Invalid personal permission");
            require(perm.medical == 0 || perm.medical == 1, "Invalid medical permission");
            require(perm.diagnosis == 0 || perm.diagnosis == 1, "Invalid diagnosis permission");
            // 更新权限映射
            emrsRoleAccess[fid][role] = perm;
        }

        return true;
    }

    /**
     * @dev 查询指定Fid和角色的权限（供EACC合约调用）
     * @param fid 电子病历唯一标识
     * @param role 角色名称
     * @return 文件权限结构体
     */
    function getPermission(bytes32 fid, string calldata role) external view returns (FilePermission memory) {
        return emrsRoleAccess[fid][role];
    }
}