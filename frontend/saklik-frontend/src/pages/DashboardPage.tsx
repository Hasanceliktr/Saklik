import React, { useState, useEffect, useCallback } from 'react';
import {
    Button, Typography, message as AntMessage, Upload, Progress,
    Row, Col, Card, Divider, Spin, Table,
    Modal, Tooltip, Input, Space
} from 'antd';
import {
    UploadOutlined, FileTextOutlined, DownloadOutlined, DeleteOutlined,
    ExclamationCircleFilled, SearchOutlined
} from '@ant-design/icons';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import apiClient from '../services/api';
import type { RcFile, UploadProps, UploadFile as AntdUploadFile } from 'antd/es/upload/interface';
import type { FileMetadata } from "../types/fileTypes";
import type { TableProps, TableColumnsType } from 'antd';

const { Title, Paragraph, Text } = Typography;
const { Search } = Input;
const { confirm } = Modal;

const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const dm = 2; // decimal places
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
};

const DashboardPage: React.FC = () => {
    // Gerekli Hook'lar (Bunlar React ve react-router-dom'dan import edilmeli)
    // const { user, isAuthenticated, logout } = useAuth(); // useAuth importu gerektirir
    // const navigate = useNavigate(); // useNavigate importu gerektirir
    // const [fileList, setFileList] = useState<AntdUploadFile[]>([]); // useState importu gerektirir
    // const [uploading, setUploading] = useState(false);
    // const [myFiles, setMyFiles] = useState<FileMetadata[]>([]);
    // const [loadingFiles, setLoadingFiles] = useState(false);
    // const [deletingFileId, setDeletingFileId] = useState<number | null>(null);
    // const [searchTerm, setSearchTerm] = useState('');

    // Yukarıdaki satırları kendi importlarına göre düzenlemelisin.
    // Şimdilik örnek olması açısından React ve diğer hook'ları kullanıyormuş gibi varsayıyorum.
    // Gerçek kullanım için:
    const { user, isAuthenticated, logout } = useAuth(); // useAuth'u import etmelisin
    const navigate = useNavigate(); // useNavigate'i import etmelisin

    const [fileList, setFileList] = useState<AntdUploadFile[]>([]);
    const [uploading, setUploading] = useState(false);
    // Genel uploadProgress state'ini kaldırmıştık, Antd'nin kendi item progress'i kullanılacak.

    const [myFiles, setMyFiles] = useState<FileMetadata[]>([]);
    const [loadingFiles, setLoadingFiles] = useState(false);
    const [deletingFileId, setDeletingFileId] = useState<number | null>(null);
    const [searchTerm, setSearchTerm] = useState('');

    const fetchMyFiles = useCallback(async () => {
        if (!user || !isAuthenticated) {
            setMyFiles([]);
            return;
        }
        setLoadingFiles(true);
        try {
            const response = await apiClient.get<FileMetadata[]>('/files'); // apiClient importu gerektirir
            setMyFiles(response.data);
        } catch (error: any) {
            AntMessage.error('Dosyalarınız yüklenirken bir hata oluştu.'); // AntMessage importu gerektirir
        } finally {
            setLoadingFiles(false);
        }
    }, [user, isAuthenticated]);

    useEffect(() => {
        if (isAuthenticated) {
            fetchMyFiles();
        } else {
            setMyFiles([]);
        }
    }, [isAuthenticated, fetchMyFiles]);

    const handleLogout = () => {
        logout();
    };

    const uploadProps: UploadProps = { // UploadProps importu gerektirir
        name: 'file',
        fileList: fileList,
        multiple: true,
        beforeUpload: (file, currentFileList) => {
            const newUploadFile: AntdUploadFile = { // AntdUploadFile importu gerektirir
                uid: file.uid,
                name: file.name,
                status: 'selected',
                originFileObj: file as RcFile, // RcFile importu gerektirir
                percent: 0,
                size: file.size,
                type: file.type,
            };
            if (!fileList.find(f => f.uid === newUploadFile.uid)) {
                setFileList(prevList => [...prevList, newUploadFile]);
            }
            return false;
        },
        onRemove: (file) => {
            setFileList(prevList => prevList.filter(item => item.uid !== file.uid));
            return true;
        },
        showUploadList: true,
    };

    const handleUpload = async () => {
        const filesToProcess = fileList.filter(f => f.status === 'selected' || f.status === 'error');
        if (filesToProcess.length === 0) {
            AntMessage.warning('Yüklenecek yeni dosya bulunmuyor.');
            return;
        }

        setUploading(true);
        let successCount = 0;
        let errorCount = 0;

        for (const fileEntry of filesToProcess) {
            if (!fileEntry.originFileObj) {
                setFileList(prev => prev.map(f => f.uid === fileEntry.uid ? { ...f, status: 'error', error: 'Dosya verisi eksik' } : f));
                errorCount++;
                continue;
            }

            const formData = new FormData();
            formData.append('file', fileEntry.originFileObj as RcFile);

            setFileList(prev => prev.map(f => f.uid === fileEntry.uid ? { ...f, status: 'uploading', percent: 0 } : f));

            try {
                // eslint-disable-next-line no-unused-vars
                const response = await apiClient.post<{ message: string }>('/files/upload', formData, {
                    onUploadProgress: (progressEvent) => {
                        if (progressEvent.total) {
                            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                            setFileList(prev =>
                                prev.map(f =>
                                    f.uid === fileEntry.uid ? { ...f, percent: percentCompleted } : f
                                )
                            );
                        }
                    },
                });
                successCount++;
                setFileList(prev => prev.map(f => f.uid === fileEntry.uid ? { ...f, status: 'done', percent: 100 } : f));
            } catch (error: any) {
                const errorMessage = error.response?.data?.message || error.message || 'Dosya yüklenirken bir hata oluştu.';
                errorCount++;
                setFileList(prev => prev.map(f => f.uid === fileEntry.uid ? { ...f, status: 'error', error: errorMessage } : f));
                console.error(`Upload error for ${fileEntry.name}:`, error.response || error);
            }
        }

        setUploading(false);
        if (successCount > 0) {
            AntMessage.success(`${successCount} dosya başarıyla yüklendi.`);
        }
        if (errorCount > 0) {
            AntMessage.error(`${errorCount} dosya yüklenirken hata oluştu. Lütfen listedeki hatalı dosyaları kontrol edin.`);
            // Hatalı dosyalar listede kalsın, başarılı olanları temizleyebiliriz veya kullanıcıya bırakabiliriz.
            // Şimdilik, SADECE başarılı olanları ve yüklenmekte olanları temizleyelim, hatalılar kalsın.
            setFileList(prev => prev.filter(f => f.status === 'error'));
        } else if (successCount > 0 && errorCount === 0) {
            // Hiç hata yoksa ve en az bir başarı varsa tüm listeyi temizle
            setFileList([]);
        }
        fetchMyFiles();
    };


    const handleDownloadFile = async (storedFileName: string, originalFileName: string) => {
        AntMessage.loading({ content: `'${originalFileName}' indiriliyor...`, key: `download-${storedFileName}`, duration: 0 });
        try {
            const response = await apiClient.get(`/files/download/${storedFileName}`, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data], { type: response.headers['content-type'] || 'application/octet-stream' }));
            const link = document.createElement('a'); link.href = url;
            link.setAttribute('download', originalFileName); document.body.appendChild(link);
            link.click(); link.parentNode?.removeChild(link); window.URL.revokeObjectURL(url);
            AntMessage.success({ content: `'${originalFileName}' başarıyla indirildi!`, key: `download-${storedFileName}`, duration: 3 });
        } catch (error: any) {
            const errorMessage = error.response?.data?.message || 'Dosya indirilirken bir hata oluştu.';
            AntMessage.error({ content: errorMessage, key: `download-${storedFileName}`, duration: 3 });
        }
    };

    const showDeleteConfirm = (fileItem: FileMetadata) => { // FileMetadata importu gerektirir
        Modal.confirm({ // Modal importu gerektirir
            title: `"${fileItem.fileName}" dosyasını silmek istediğinize emin misiniz?`,
            icon: <ExclamationCircleFilled />, // ExclamationCircleFilled importu gerektirir
            content: 'Bu işlem geri alınamaz ve dosyanız kalıcı olarak silinecektir.',
            okText: 'Evet, Sil', okType: 'danger', cancelText: 'Hayır, İptal Et',
            maskClosable: true,
            async onOk() {
                try { await handleDeleteFile(fileItem); } catch (error) { console.error("Silme modal onOk hatası:", error); }
            },
        });
    };

    const handleDeleteFile = async (fileToDelete: FileMetadata) => {
        setDeletingFileId(fileToDelete.id);
        try {
            const response = await apiClient.delete<{ message: string }>(`/files/${fileToDelete.storedFileName}`);
            AntMessage.success(response.data.message || `'${fileToDelete.fileName}' başarıyla silindi.`, 3);
            fetchMyFiles();
        } catch (error: any) {
            const errorMessage = error.response?.data?.message || `'${fileToDelete.fileName}' silinirken bir hata oluştu.`;
            AntMessage.error(errorMessage, 5);
        } finally { setDeletingFileId(null); }
    };

    const columns: TableColumnsType<FileMetadata> = [ // TableColumnsType ve FileMetadata importu gerektirir
        {
            title: 'Dosya Adı', dataIndex: 'fileName', key: 'fileName',
            sorter: (a, b) => a.fileName.localeCompare(b.fileName),
            render: (text, record) => (
                <Space> {/* Space importu gerektirir */}
                    <FileTextOutlined /> {/* FileTextOutlined importu gerektirir */}
                    <a onClick={() => handleDownloadFile(record.storedFileName, record.fileName)} title="İndir">{text}</a>
                </Space>
            ),
        },
        {
            title: 'Boyut', dataIndex: 'size', key: 'size',
            sorter: (a, b) => a.size - b.size,
            render: (size: number) => formatFileSize(size), align: 'right',
        },
        {
            title: 'Yüklenme Tarihi', dataIndex: 'uploadedAt', key: 'uploadedAt',
            sorter: (a, b) => new Date(a.uploadedAt).getTime() - new Date(b.uploadedAt).getTime(),
            render: (uploadedAt: string) => new Date(uploadedAt).toLocaleDateString('tr-TR', {
                year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
            }),
            align: 'center',
        },
        {
            title: 'İşlemler', key: 'actions', align: 'center',
            render: (_, record) => (
                <Space size="middle">
                    <Tooltip title="İndir"> {/* Tooltip importu gerektirir */}
                        <Button type="text" icon={<DownloadOutlined />} /* DownloadOutlined importu */
                                onClick={() => handleDownloadFile(record.storedFileName, record.fileName)}
                                loading={deletingFileId === record.id && deletingFileId !== null} // İndirme için ayrı loading state daha iyi olur
                        />
                    </Tooltip>
                    <Tooltip title="Sil">
                        <Button type="text" danger icon={<DeleteOutlined />} /* DeleteOutlined importu */
                                onClick={() => showDeleteConfirm(record)}
                                loading={deletingFileId === record.id}
                                disabled={deletingFileId !== null && deletingFileId !== record.id}
                        />
                    </Tooltip>
                </Space>
            ),
        },
    ];

    const filteredFiles = myFiles.filter(file =>
        file.fileName.toLowerCase().includes(searchTerm.toLowerCase())
    );

    if (!user && !isAuthenticated && !loadingFiles) { navigate('/login'); return null; }
    if (!user && loadingFiles) { return (<Row justify="center" align="middle" style={{ minHeight: '100vh' }}><Col><Spin size="large" tip="Kullanıcı bilgileri yükleniyor..." /></Col></Row>); /* Row, Col, Spin importu */}

    return (
        <Row justify="center" style={{ paddingTop: '50px', paddingBottom: '50px', background: '#f0f2f5', minHeight: 'calc(100vh - 100px)' }}>
            <Col xs={23} sm={22} md={20} lg={18} xl={16}>
                <Card variant="outlined" style={{ boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}> {/* Card, Row, Col, Title, Text, Paragraph, Divider, Upload, Button, Search, Spin, Table importları */}
                    <Row justify="space-between" align="middle" style={{ marginBottom: '20px' }}>
                        <Col> {user && (<Title level={3} style={{ margin: 0 }}>Hoş Geldiniz, <Text strong>{user.username}</Text>!</Title>)}</Col>
                        <Col><Button onClick={handleLogout} danger>Çıkış Yap</Button></Col>
                    </Row>
                    {user && (<Paragraph style={{ marginBottom: '30px' }}>Kişisel Saklık alanınız. Dosyalarınızı güvenle yönetin.</Paragraph>)}
                    <Divider />
                    <Title level={4} style={{ marginTop: '30px', marginBottom: '20px' }}>Yeni Dosya(lar) Yükle</Title>
                    <Upload {...uploadProps}>
                        <Button icon={<UploadOutlined />} style={{width: '100%'}} size="large">
                            Yüklenecek Dosyaları Seçin
                        </Button>
                    </Upload>
                    {fileList.length > 0 && !uploading && fileList.some(f => f.status === 'selected' || f.status === 'error') && (
                        <Button
                            type="primary"
                            onClick={handleUpload}
                            style={{ marginTop: 16, width: '100%' }}
                            size="large"
                        >
                            {`Seçili ${fileList.filter(f => f.status !== 'done').length} Dosyayı Yükle`}
                        </Button>
                    )}
                    {uploading && ( // Genel bir yükleme göstergesi yerine, Antd Upload listesi her dosya için kendi progress'ini gösterir.
                        // Ama yine de tüm işlemler bitene kadar bir şeyler göstermek isteyebiliriz.
                        <div style={{textAlign: 'center', marginTop: '10px'}}>
                            <Spin tip={`Dosyalar yükleniyor... (${fileList.filter(f => f.status === 'done').length}/${fileList.length} tamamlandı)`} />
                        </div>
                    )}
                    <Divider style={{ marginTop: '40px', marginBottom: '30px' }} />
                    <Title level={4} style={{ marginBottom: '20px' }}>Dosyalarım</Title>
                    <Search
                        placeholder="Dosyalarımda ara..."
                        onChange={(e) => setSearchTerm(e.target.value)}
                        style={{ marginBottom: 20, width: '100%', maxWidth: '400px' }}
                        allowClear
                        size="large"
                    />
                    <Spin spinning={loadingFiles || deletingFileId !== null} tip={deletingFileId ? "Dosya siliniyor..." : "Dosyalarınız yükleniyor..."}>
                        <Table
                            columns={columns}
                            dataSource={filteredFiles}
                            rowKey="id"
                            locale={{ emptyText: "Henüz hiç dosya yüklemediniz veya aramanızla eşleşen dosya bulunamadı." }}
                            pagination={{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '20'], showTotal: (total, range) => `${range[0]}-${range[1]} / ${total} dosya` }}
                            scroll={{ x: 'max-content' }}
                        />
                    </Spin>
                </Card>
            </Col>
        </Row>
    );
};


export default DashboardPage;